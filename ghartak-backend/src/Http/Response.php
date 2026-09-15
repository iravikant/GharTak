<?php

declare(strict_types=1);

namespace App\Http;

/**
 * Every endpoint returns this same envelope shape (spec section 16):
 * { "success": bool, "message": string, "data": mixed } or
 * { "success": false, "message": string, "error_code": string }
 */
final class Response
{
    public static function success(mixed $data = null, string $message = 'OK', int $status = 200): never
    {
        self::send($status, [
            'success' => true,
            'message' => $message,
            'data' => $data,
        ]);
    }

    public static function error(string $message, string $errorCode = 'ERROR', int $status = 400): never
    {
        self::send($status, [
            'success' => false,
            'message' => $message,
            'error_code' => $errorCode,
        ]);
    }

    private static function send(int $status, array $payload): never
    {
        http_response_code($status);
        header('Content-Type: application/json');
        echo json_encode($payload, JSON_UNESCAPED_SLASHES);
        exit;
    }
}
