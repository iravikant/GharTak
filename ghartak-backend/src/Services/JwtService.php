<?php

declare(strict_types=1);

namespace App\Services;

use Firebase\JWT\JWT;
use Firebase\JWT\Key;
use Firebase\JWT\ExpiredException;

final class JwtService
{
    private string $secret;
    private int $ttlMinutes;

    public function __construct()
    {
        $this->secret = $_ENV['JWT_SECRET'] ?? throw new \RuntimeException('JWT_SECRET is not configured');
        $this->ttlMinutes = (int) ($_ENV['JWT_TTL_MINUTES'] ?? 10080);
    }

    public function issue(string $userId, string $role): string
    {
        $now = time();
        $payload = [
            'sub' => $userId,
            'role' => $role,
            'iat' => $now,
            'exp' => $now + ($this->ttlMinutes * 60),
        ];
        return JWT::encode($payload, $this->secret, 'HS256');
    }

    /**
     * @return array{sub:string, role:string}|null null when the token is missing, malformed, or expired
     */
    public function verify(string $token): ?array
    {
        try {
            $decoded = JWT::decode($token, new Key($this->secret, 'HS256'));
            return ['sub' => $decoded->sub, 'role' => $decoded->role];
        } catch (ExpiredException) {
            return null;
        } catch (\Throwable) {
            return null;
        }
    }
}
