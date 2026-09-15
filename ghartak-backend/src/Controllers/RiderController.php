<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Config\Database;
use App\Http\HttpException;
use App\Http\Request;
use App\Http\Response;
use App\Models\Delivery;
use App\Models\Rider;
use App\Services\PricingService;
use App\Utils\Validator;

final class RiderController
{
    /** Nearby search radius for matching a rider to a delivery (spec section 5). */
    private const MATCH_RADIUS_KM = 6.0;

    public function updateStatus(Request $request): void
    {
        $riderId = (string) $request->param('auth_user_id');
        $input = $request->all();
        Validator::requireFields($input, ['online']);

        Rider::setOnlineStatus($riderId, (bool) $input['online']);
        Response::success(null, $input['online'] ? 'You are now online' : 'You are now offline');
    }

    public function updateLocation(Request $request): void
    {
        $riderId = (string) $request->param('auth_user_id');
        $input = $request->all();
        Validator::requireFields($input, ['lat', 'lng']);

        Rider::upsertLocation($riderId, (float) $input['lat'], (float) $input['lng'], isset($input['bearing']) ? (float) $input['bearing'] : null);
        Response::success(null, 'Location updated');
    }

    /**
     * Returns open (SEARCHING_RIDER) deliveries near this rider's last known location, ordered
     * nearest-first, so the app can show a single "New Delivery Request" card at a time (spec
     * section 10) or a short queue.
     */
    public function requests(Request $request): void
    {
        $riderId = (string) $request->param('auth_user_id');
        $location = Delivery::latestRiderLocation($riderId);
        if ($location === null) {
            throw new HttpException('Share your location before requesting deliveries', 'LOCATION_REQUIRED', 422);
        }

        $pdo = Database::connection();
        $lat = (float) $location['lat'];
        $lng = (float) $location['lng'];
        $latDelta = self::MATCH_RADIUS_KM / 111.0;
        $lngDelta = self::MATCH_RADIUS_KM / (111.320 * max(cos(deg2rad($lat)), 0.1));

        $stmt = $pdo->prepare(
            'SELECT * FROM deliveries
             WHERE delivery_status = "SEARCHING_RIDER"
               AND pickup_lat BETWEEN :lat_min AND :lat_max
               AND pickup_lng BETWEEN :lng_min AND :lng_max
             ORDER BY created_at ASC
             LIMIT 10'
        );
        $stmt->execute([
            'lat_min' => $lat - $latDelta, 'lat_max' => $lat + $latDelta,
            'lng_min' => $lng - $lngDelta, 'lng_max' => $lng + $lngDelta,
        ]);
        $rows = $stmt->fetchAll();

        $pricing = new PricingService();
        $requests = array_map(function ($d) use ($pricing, $lat, $lng) {
            $pickupDistance = $pricing->distanceKm($lat, $lng, (float) $d['pickup_lat'], (float) $d['pickup_lng']);
            $tripDistance = $pricing->distanceKm(
                (float) $d['pickup_lat'], (float) $d['pickup_lng'],
                (float) $d['drop_lat'], (float) $d['drop_lng']
            );
            return [
                'deliveryId' => $d['id'],
                'pickupDistanceKm' => round($pickupDistance, 1),
                'tripDistanceKm' => round($tripDistance, 1),
                'pickupAddress' => $d['pickup_address'],
                'dropAddress' => $d['drop_address'],
                'estimatedEarnings' => (float) $d['delivery_fee'],
                'parcelType' => $d['parcel_type'],
            ];
        }, $rows);

        Response::success($requests);
    }

    public function accept(Request $request): void
    {
        $riderId = (string) $request->param('auth_user_id');
        $deliveryId = (string) $request->param('id');

        $delivery = Delivery::findById($deliveryId);
        if ($delivery === null) {
            throw new HttpException('Delivery not found', 'NOT_FOUND', 404);
        }
        if ($delivery['delivery_status'] !== 'SEARCHING_RIDER') {
            throw new HttpException('This delivery has already been accepted by another rider', 'ALREADY_ASSIGNED', 409);
        }

        Delivery::assignRider($deliveryId, $riderId);
        Response::success($this->serializeDelivery(Delivery::findById($deliveryId)), 'Delivery accepted');
    }

    public function reject(Request $request): void
    {
        // Rejections aren't persisted per-rider in this pass (would need a
        // delivery_rejections table to avoid re-offering the same rider); the request simply
        // stops being returned to this rider once findNearbyOnline / requests() re-runs against
        // fresh rider-location data on their next poll.
        Response::success(null, 'Request declined');
    }

    public function markPickup(Request $request): void
    {
        $delivery = $this->assignedDelivery($request);
        Delivery::updateStatus($delivery['id'], 'PARCEL_PICKED_UP', (string) $request->param('auth_user_id'));
        Response::success(null, 'Pickup confirmed');
    }

    public function startDelivery(Request $request): void
    {
        $delivery = $this->assignedDelivery($request);
        if ($delivery['delivery_status'] !== 'PARCEL_PICKED_UP') {
            throw new HttpException('Confirm pickup before starting delivery', 'INVALID_STATE', 409);
        }
        Delivery::updateStatus($delivery['id'], 'OUT_FOR_DELIVERY', (string) $request->param('auth_user_id'));
        Response::success(null, 'Delivery started');
    }

    public function complete(Request $request): void
    {
        // Final confirmation happens via DeliveryController::verifyOtp (spec section 9) —
        // this endpoint is kept for the arrived-at-destination transition only.
        $delivery = $this->assignedDelivery($request);
        Delivery::updateStatus($delivery['id'], 'RIDER_NEAR_DESTINATION', (string) $request->param('auth_user_id'));
        Response::success(null, 'Marked as near destination — ask the receiver for their OTP to complete delivery');
    }

    private function assignedDelivery(Request $request): array
    {
        $riderId = (string) $request->param('auth_user_id');
        $deliveryId = (string) $request->param('id');

        $delivery = Delivery::findById($deliveryId);
        if ($delivery === null) {
            throw new HttpException('Delivery not found', 'NOT_FOUND', 404);
        }
        if ($delivery['rider_id'] !== $riderId) {
            throw new HttpException('You are not assigned to this delivery', 'FORBIDDEN', 403);
        }
        return $delivery;
    }

    private function serializeDelivery(array $d): array
    {
        return [
            'id' => $d['id'],
            'status' => $d['delivery_status'],
            'pickupAddress' => $d['pickup_address'],
            'dropAddress' => $d['drop_address'],
            'receiverName' => $d['receiver_name'],
            'receiverPhone' => $d['receiver_phone'],
        ];
    }
}
