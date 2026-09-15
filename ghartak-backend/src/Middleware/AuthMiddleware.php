<?php

declare(strict_types=1);

namespace App\Middleware;

use App\Http\HttpException;
use App\Http\Request;
use App\Services\JwtService;

/**
 * Usage: $router->get('/api/customer/profile', [CustomerController::class, 'profile'], [new AuthMiddleware()]);
 * On success, stores 'auth_user_id' and 'auth_role' as request params so controllers can read
 * them via $request->param('auth_user_id').
 */
final class AuthMiddleware
{
    public function __construct(private readonly ?array $allowedRoles = null)
    {
    }

    public function __invoke(Request $request): void
    {
        $token = $request->bearerToken();
        if ($token === null) {
            throw new HttpException('Authentication required', 'UNAUTHENTICATED', 401);
        }

        $claims = (new JwtService())->verify($token);
        if ($claims === null) {
            throw new HttpException('Invalid or expired session, please log in again', 'INVALID_TOKEN', 401);
        }

        if ($this->allowedRoles !== null && !in_array($claims['role'], $this->allowedRoles, true)) {
            throw new HttpException('You are not authorized to perform this action', 'FORBIDDEN', 403);
        }

        $request->mergeParams([
            'auth_user_id' => $claims['sub'],
            'auth_role' => $claims['role'],
        ]);
    }
}
