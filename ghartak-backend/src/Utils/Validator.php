<?php

declare(strict_types=1);

namespace App\Utils;

use App\Http\HttpException;

final class Validator
{
    /**
     * @param array<string> $fields
     * @throws HttpException when any required field is missing or blank
     */
    public static function requireFields(array $input, array $fields): void
    {
        $missing = [];
        foreach ($fields as $field) {
            $value = $input[$field] ?? null;
            if ($value === null || (is_string($value) && trim($value) === '')) {
                $missing[] = $field;
            }
        }
        if (!empty($missing)) {
            throw new HttpException(
                'Missing required field(s): ' . implode(', ', $missing),
                'VALIDATION_ERROR',
                422
            );
        }
    }

    public static function isValidPhone(string $phone): bool
    {
        return (bool) preg_match('/^\+?[0-9]{8,15}$/', $phone);
    }

    public static function isValidEmail(?string $email): bool
    {
        if ($email === null || $email === '') {
            return true; // email is optional
        }
        return filter_var($email, FILTER_VALIDATE_EMAIL) !== false;
    }
}
