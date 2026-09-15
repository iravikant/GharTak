<?php

declare(strict_types=1);

namespace App\Services;

/**
 * Delivery OTP (spec section 9 + 20): generated server-side with a CSPRNG, never
 * derived from anything guessable (not a timestamp, not the delivery id).
 */
final class OtpService
{
    private const LENGTH = 4;
    private const TTL_SECONDS = 3600; // valid for the duration of the active delivery

    public function generate(): array
    {
        $otp = str_pad((string) random_int(0, (10 ** self::LENGTH) - 1), self::LENGTH, '0', STR_PAD_LEFT);
        return [
            'otp' => $otp,
            'expiresAt' => time() + self::TTL_SECONDS,
        ];
    }

    public function isValid(string $submitted, ?string $stored, ?int $expiresAt): bool
    {
        if ($stored === null || $expiresAt === null) {
            return false;
        }
        if (time() > $expiresAt) {
            return false;
        }
        return hash_equals($stored, $submitted);
    }
}
