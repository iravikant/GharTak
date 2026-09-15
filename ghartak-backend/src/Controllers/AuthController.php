<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Http\HttpException;
use App\Http\Request;
use App\Http\Response;
use App\Models\User;
use App\Services\JwtService;
use App\Utils\Validator;

final class AuthController
{
    public function register(Request $request): void
    {
        $input = $request->all();
        Validator::requireFields($input, ['name', 'phone', 'password', 'role']);

        $name = trim((string) $input['name']);
        $phone = trim((string) $input['phone']);
        $email = isset($input['email']) && $input['email'] !== '' ? trim((string) $input['email']) : null;
        $password = (string) $input['password'];
        $role = strtolower((string) $input['role']);

        if (!Validator::isValidPhone($phone)) {
            throw new HttpException('Enter a valid phone number', 'VALIDATION_ERROR', 422);
        }
        if (!Validator::isValidEmail($email)) {
            throw new HttpException('Enter a valid email address', 'VALIDATION_ERROR', 422);
        }
        if (strlen($password) < 6) {
            throw new HttpException('Password must be at least 6 characters', 'VALIDATION_ERROR', 422);
        }
        if (!in_array($role, ['customer', 'rider'], true)) {
            throw new HttpException('Role must be customer or rider', 'VALIDATION_ERROR', 422);
        }
        if (User::phoneExists($phone)) {
            throw new HttpException('An account with this phone number already exists', 'PHONE_TAKEN', 409);
        }

        $passwordHash = password_hash($password, PASSWORD_BCRYPT);
        $user = User::create($name, $phone, $email, $passwordHash, $role);

        $token = (new JwtService())->issue($user['id'], $user['role']);

        Response::success([
            'token' => $token,
            'user' => $this->serializeUser($user),
        ], 'Registration successful', 201);
    }

    public function login(Request $request): void
    {
        $input = $request->all();
        Validator::requireFields($input, ['identifier', 'password']);

        $identifier = trim((string) $input['identifier']);
        $password = (string) $input['password'];

        $user = User::findByPhoneOrEmail($identifier);
        if ($user === null || !password_verify($password, $user['password_hash'])) {
            // Same message for "no such user" and "wrong password" — never reveal which.
            throw new HttpException('Invalid phone/email or password', 'INVALID_CREDENTIALS', 401);
        }

        if ($user['status'] === 'suspended') {
            throw new HttpException('This account has been suspended. Contact support.', 'ACCOUNT_SUSPENDED', 403);
        }

        $token = (new JwtService())->issue($user['id'], $user['role']);

        Response::success([
            'token' => $token,
            'user' => $this->serializeUser($user),
        ], 'Login successful');
    }

    public function logout(Request $request): void
    {
        // Stateless JWT — nothing to invalidate server-side without a token blocklist.
        // The Android client drops its stored token; this endpoint exists so the client
        // has a single place to call and so a blocklist can be added later without an API change.
        Response::success(null, 'Logged out');
    }

    private function serializeUser(array $user): array
    {
        return [
            'id' => $user['id'],
            'name' => $user['name'],
            'phone' => $user['phone'],
            'email' => $user['email'],
            'role' => $user['role'],
            'profilePhotoUrl' => $user['profile_photo_url'],
            'rating' => null,
        ];
    }
}
