<?php

declare(strict_types=1);

namespace App\Services;

/**
 * Computes the price breakdown shown to the customer before booking (spec section 4).
 * Pricing is intentionally simple (base fare + per-km + flat platform fee) and reads its
 * numbers from env/config rather than hard-coding them, so admins can tune pricing without
 * a code deploy once pricing_rules-backed lookups are wired in.
 */
final class PricingService
{
    private const PARCEL_TYPE_MULTIPLIER = [
        'DOCUMENTS' => 0.9,
        'SMALL_PACKAGE' => 1.0,
        'MEDIUM_PACKAGE' => 1.15,
        'LARGE_PACKAGE' => 1.35,
        'FOOD' => 1.0,
        'ELECTRONICS' => 1.25,
        'OTHER' => 1.0,
    ];

    public function __construct(
        private readonly float $baseFare = 25.0,
        private readonly float $perKmRate = 8.0,
        private readonly float $platformFee = 10.0
    ) {
    }

    public static function fromEnv(): self
    {
        return new self(
            baseFare: (float) ($_ENV['PRICING_BASE_FARE'] ?? 25),
            perKmRate: (float) ($_ENV['PRICING_PER_KM'] ?? 8),
            platformFee: (float) ($_ENV['PRICING_PLATFORM_FEE'] ?? 10)
        );
    }

    /** Haversine distance in km between two lat/lng points. */
    public function distanceKm(float $lat1, float $lng1, float $lat2, float $lng2): float
    {
        $earthRadiusKm = 6371.0;
        $dLat = deg2rad($lat2 - $lat1);
        $dLng = deg2rad($lng2 - $lng1);
        $a = sin($dLat / 2) ** 2
            + cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * sin($dLng / 2) ** 2;
        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
        return $earthRadiusKm * $c;
    }

    /**
     * @return array{distanceKm: float, deliveryFee: float, platformFee: float, totalAmount: float, estimatedMinutes: int}
     */
    public function quote(float $pickupLat, float $pickupLng, float $dropLat, float $dropLng, string $parcelType): array
    {
        $distanceKm = $this->distanceKm($pickupLat, $pickupLng, $dropLat, $dropLng);
        $multiplier = self::PARCEL_TYPE_MULTIPLIER[$parcelType] ?? 1.0;

        $deliveryFee = round(($this->baseFare + ($distanceKm * $this->perKmRate)) * $multiplier, 2);
        $platformFee = round($this->platformFee, 2);

        // Rough average of 25 km/h city-traffic speed for local parcel delivery.
        $estimatedMinutes = max(5, (int) ceil(($distanceKm / 25) * 60));

        return [
            'distanceKm' => round($distanceKm, 2),
            'deliveryFee' => $deliveryFee,
            'platformFee' => $platformFee,
            'totalAmount' => round($deliveryFee + $platformFee, 2),
            'estimatedMinutes' => $estimatedMinutes,
        ];
    }
}
