<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Http\HttpException;
use App\Http\Request;
use App\Http\Response;
use App\Models\User;

final class CustomerController
{
    public function profile(Request $request): void
    {
        $user = User::findById((string) $request->param('auth_user_id'));
        if ($user === null) {
            throw new HttpException('User not found', 'NOT_FOUND', 404);
        }

        Response::success([
            'id' => $user['id'],
            'name' => $user['name'],
            'phone' => $user['phone'],
            'email' => $user['email'],
            'profilePhotoUrl' => $user['profile_photo_url'],
        ]);
    }

    public function updateProfile(Request $request): void
    {
        $userId = (string) $request->param('auth_user_id');
        $input = $request->all();

        $pdo = \App\Config\Database::connection();
        $stmt = $pdo->prepare(
            'UPDATE users SET name = COALESCE(:name, name), profile_photo_url = COALESCE(:photo, profile_photo_url), updated_at = :updated_at WHERE id = :id'
        );
        $stmt->execute([
            'name' => $input['name'] ?? null,
            'photo' => $input['profilePhotoUrl'] ?? null,
            'updated_at' => time(),
            'id' => $userId,
        ]);

        Response::success(null, 'Profile updated');
    }
}
