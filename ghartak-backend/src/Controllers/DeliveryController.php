<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Http\HttpException;
use App\Http\Request;
use App\Http\Response;
use App\Models\Delivery;
use App\Services\OtpService;
use App\Services\PricingService;
use App\Utils\Validator;

final class DeliveryController
{
    public function create(Request $request): void
    {
        $customerId = (string) $request->param('auth_user_id');
        $input = $request->all();

        Validator::requireFields($input, ['pickup', 'drop', 'receiverName', 'receiverPhone', 'parcelType']);
        $pickup = $input['pickup'];
        $drop = $input['drop'];
        Validator::requireFields($pickup, ['lat', 'lng', 'formattedAddress']);
        Validator::requireFields($drop, ['lat', 'lng', 'formattedAddress']);

        if (!Validator::isValidPhone((string) $input['receiverPhone'])) {
            throw new HttpException('Enter a valid receiver phone number', 'VALIDATION_ERROR', 422);
        }

        $pricing = PricingService::fromEnv();
        $quote = $pricing->quote(
            (float) $pickup['lat'],
            (float) $pickup['lng'],
            (float) $drop['lat'],
            (float) $drop['lng'],
            strtoupper((string) $input['parcelType'])
        );

        $delivery = Delivery::create([
            'customerId' => $customerId,
            'pickup' => $pickup,
            'drop' => $drop,
            'receiverName' => $input['receiverName'],
            'receiverPhone' => $input['receiverPhone'],
            'parcelType' => strtoupper((string) $input['parcelType']),
            'parcelWeightKg' => $input['parcelWeightKg'] ?? null,
            'parcelDescription' => $input['parcelDescription'] ?? null,
            'parcelImageUrl' => $input['parcelImageUrl'] ?? null,
            'deliveryFee' => $quote['deliveryFee'],
            'platformFee' => $quote['platformFee'],
            'totalAmount' => $quote['totalAmount'],
        ]);

        [$otp, $expiresAt] = array_values((new OtpService())->generate());
        Delivery::setOtp($delivery['id'], $otp, $expiresAt);

        Response::success($this->serialize(Delivery::findById($delivery['id'])), 'Delivery created successfully', 201);
    }

    public function history(Request $request): void
    {
        $customerId = (string) $request->param('auth_user_id');
        $page = max(1, (int) $request->query('page', 1));
        $pageSize = min(50, max(1, (int) $request->query('pageSize', 20)));

        $rows = Delivery::historyForCustomer($customerId, $page, $pageSize);
        Response::success(array_map($this->serialize(...), $rows));
    }

    public function show(Request $request): void
    {
        $delivery = $this->authorizedDelivery($request);
        Response::success($this->serialize($delivery));
    }

    public function cancel(Request $request): void
    {
        $delivery = $this->authorizedDelivery($request);

        if (in_array($delivery['delivery_status'], ['DELIVERED', 'CANCELLED', 'FAILED'], true)) {
            throw new HttpException('This delivery can no longer be cancelled', 'INVALID_STATE', 409);
        }
        if (in_array($delivery['delivery_status'], ['OUT_FOR_DELIVERY', 'RIDER_NEAR_DESTINATION'], true)) {
            throw new HttpException('Delivery already out for delivery and cannot be cancelled', 'INVALID_STATE', 409);
        }

        Delivery::updateStatus($delivery['id'], 'CANCELLED', (string) $request->param('auth_user_id'));
        Response::success(null, 'Delivery cancelled');
    }

    public function tracking(Request $request): void
    {
        $delivery = $this->authorizedDelivery($request);

        $riderLocation = null;
        if ($delivery['rider_id'] !== null) {
            $loc = Delivery::latestRiderLocation($delivery['rider_id']);
            if ($loc !== null) {
                $riderLocation = [
                    'riderId' => $loc['rider_id'],
                    'deliveryId' => $delivery['id'],
                    'lat' => (float) $loc['lat'],
                    'lng' => (float) $loc['lng'],
                    'bearing' => $loc['bearing'] !== null ? (float) $loc['bearing'] : null,
                    'timestamp' => (int) $loc['updated_at'],
                ];
            }
        }

        Response::success([
            'delivery' => $this->serialize($delivery),
            'riderLocation' => $riderLocation,
        ]);
    }

    public function verifyOtp(Request $request): void
    {
        $delivery = $this->authorizedDeliveryForRider($request);
        $input = $request->all();
        Validator::requireFields($input, ['otp']);

        $otpService = new OtpService();
        $submitted = (string) $input['otp'];
        $expiresAt = $delivery['otp_expires_at'] !== null ? (int) $delivery['otp_expires_at'] : null;

        if (!$otpService->isValid($submitted, $delivery['otp'], $expiresAt)) {
            throw new HttpException('Invalid or expired OTP', 'INVALID_OTP', 422);
        }

        Delivery::updateStatus($delivery['id'], 'DELIVERED', (string) $request->param('auth_user_id'));
        Response::success($this->serialize(Delivery::findById($delivery['id'])), 'Delivery marked as delivered');
    }

    private function authorizedDelivery(Request $request): array
    {
        $id = (string) $request->param('id');
        $delivery = Delivery::findById($id);
        if ($delivery === null) {
            throw new HttpException('Delivery not found', 'NOT_FOUND', 404);
        }

        $authUserId = (string) $request->param('auth_user_id');
        $authRole = (string) $request->param('auth_role');

        $isOwnerCustomer = $authRole === 'customer' && $delivery['customer_id'] === $authUserId;
        $isAssignedRider = $authRole === 'rider' && $delivery['rider_id'] === $authUserId;

        if (!$isOwnerCustomer && !$isAssignedRider) {
            throw new HttpException('You do not have access to this delivery', 'FORBIDDEN', 403);
        }

        return $delivery;
    }

    private function authorizedDeliveryForRider(Request $request): array
    {
        $delivery = $this->authorizedDelivery($request);
        if ((string) $request->param('auth_role') !== 'rider') {
            throw new HttpException('Only the assigned rider can verify delivery OTP', 'FORBIDDEN', 403);
        }
        return $delivery;
    }

    private function serialize(array $d): array
    {
        return [
            'id' => $d['id'],
            'customerId' => $d['customer_id'],
            'riderId' => $d['rider_id'],
            'pickup' => [
                'lat' => (float) $d['pickup_lat'],
                'lng' => (float) $d['pickup_lng'],
                'formattedAddress' => $d['pickup_address'],
            ],
            'drop' => [
                'lat' => (float) $d['drop_lat'],
                'lng' => (float) $d['drop_lng'],
                'formattedAddress' => $d['drop_address'],
            ],
            'receiverName' => $d['receiver_name'],
            'receiverPhone' => $d['receiver_phone'],
            'parcelType' => $d['parcel_type'],
            'parcelWeightKg' => $d['parcel_weight'] !== null ? (float) $d['parcel_weight'] : null,
            'parcelDescription' => $d['parcel_description'],
            'parcelImageUrl' => $d['parcel_image_url'],
            'deliveryFee' => (float) $d['delivery_fee'],
            'platformFee' => (float) $d['platform_fee'],
            'totalAmount' => (float) $d['total_amount'],
            'status' => $d['delivery_status'],
            'paymentStatus' => $d['payment_status'],
            'createdAt' => (int) $d['created_at'],
            'acceptedAt' => $d['accepted_at'] !== null ? (int) $d['accepted_at'] : null,
            'pickedUpAt' => $d['picked_up_at'] !== null ? (int) $d['picked_up_at'] : null,
            'deliveredAt' => $d['delivered_at'] !== null ? (int) $d['delivered_at'] : null,
            // OTP is deliberately left out of this general payload — never bundle it in
            // where it could be logged or shown to the rider (spec section 20). Surface it
            // to the customer only through its own field/endpoint if a screen needs it.
        ];
    }
}
