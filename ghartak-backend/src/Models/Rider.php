<?php

declare(strict_types=1);

namespace App\Models;

use App\Config\Database;

final class Rider
{
    public static function setOnlineStatus(string $riderId, bool $isOnline): void
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare('UPDATE riders SET is_online = :online WHERE user_id = :id');
        $stmt->execute(['online' => $isOnline ? 1 : 0, 'id' => $riderId]);
    }

    public static function upsertLocation(string $riderId, float $lat, float $lng, ?float $bearing): void
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare(
            'INSERT INTO rider_locations (rider_id, lat, lng, bearing, updated_at)
             VALUES (:rider_id, :lat, :lng, :bearing, :updated_at)
             ON DUPLICATE KEY UPDATE lat = :lat2, lng = :lng2, bearing = :bearing2, updated_at = :updated_at2'
        );
        $now = time();
        $stmt->execute([
            'rider_id' => $riderId,
            'lat' => $lat, 'lng' => $lng, 'bearing' => $bearing, 'updated_at' => $now,
            'lat2' => $lat, 'lng2' => $lng, 'bearing2' => $bearing, 'updated_at2' => $now,
        ]);
    }

    /**
     * Naive nearest-online-rider lookup using a bounding-box pre-filter then exact Haversine
     * ranking in PHP. Fine at the scale of a local/city service area (spec section 5); if this
     * ever needs to scale beyond a few thousand concurrent online riders, replace the bounding
     * box + PHP sort with a spatial index (e.g. MySQL ST_Distance_Sphere or a geo cache like Redis).
     *
     * @return array<int, array{rider_id:string, lat:float, lng:float, distanceKm:float}>
     */
    public static function findNearbyOnline(float $lat, float $lng, float $radiusKm, int $limit = 10): array
    {
        $pdo = Database::connection();

        // ~1 degree latitude ≈ 111km; build a generous bounding box, then refine exactly.
        $latDelta = $radiusKm / 111.0;
        $lngDelta = $radiusKm / (111.320 * max(cos(deg2rad($lat)), 0.1));

        $stmt = $pdo->prepare(
            'SELECT rl.rider_id, rl.lat, rl.lng
             FROM rider_locations rl
             INNER JOIN riders r ON r.user_id = rl.rider_id
             WHERE r.is_online = 1
               AND r.approval_status = "approved"
               AND rl.lat BETWEEN :lat_min AND :lat_max
               AND rl.lng BETWEEN :lng_min AND :lng_max'
        );
        $stmt->execute([
            'lat_min' => $lat - $latDelta,
            'lat_max' => $lat + $latDelta,
            'lng_min' => $lng - $lngDelta,
            'lng_max' => $lng + $lngDelta,
        ]);
        $candidates = $stmt->fetchAll();

        $pricing = new \App\Services\PricingService();
        $ranked = array_map(function ($row) use ($pricing, $lat, $lng) {
            $row['distanceKm'] = $pricing->distanceKm($lat, $lng, (float) $row['lat'], (float) $row['lng']);
            return $row;
        }, $candidates);

        usort($ranked, fn ($a, $b) => $a['distanceKm'] <=> $b['distanceKm']);

        return array_slice(
            array_filter($ranked, fn ($r) => $r['distanceKm'] <= $radiusKm),
            0,
            $limit
        );
    }

    public static function findById(string $riderId): ?array
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare(
            'SELECT u.*, r.approval_status, r.is_online, r.rating_avg, r.total_deliveries
             FROM users u INNER JOIN riders r ON r.user_id = u.id
             WHERE u.id = :id LIMIT 1'
        );
        $stmt->execute(['id' => $riderId]);
        $row = $stmt->fetch();
        return $row ?: null;
    }
}
