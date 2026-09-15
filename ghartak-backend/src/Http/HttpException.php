<?php

declare(strict_types=1);

namespace App\Http;

/**
 * Throw this from a controller/service/middleware to short-circuit straight to a JSON error
 * response, e.g. `throw new HttpException('Invalid OTP', 'INVALID_OTP', 422);`
 */
final class HttpException extends \RuntimeException
{
    public function __construct(
        string $message,
        public readonly string $errorCode = 'ERROR',
        int $statusCode = 400
    ) {
        parent::__construct($message, $statusCode);
    }
}
