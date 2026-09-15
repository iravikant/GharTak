<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Config\Database;
use App\Http\HttpException;
use App\Http\Request;
use App\Http\Response;
use App\Models\Delivery;
use App\Models\User;
use App\Utils\Validator;

final class RatingComplaintController
{
    public function rate(Request $request): void
    {
        $input = $request->all();
        Validator::requireFields($input, ['deliveryId', 'ratedUser', 'score']);

        $score = (int) $input['score'];
        if ($score < 1 || $score > 5) {
            throw new HttpException('Score must be between 1 and 5', 'VALIDATION_ERROR', 422);
        }

        $delivery = Delivery::findById((string) $input['deliveryId']);
        if ($delivery === null) {
            throw new HttpException('Delivery not found', 'NOT_FOUND', 404);
        }

        $pdo = Database::connection();
        $stmt = $pdo->prepare(
            'INSERT INTO ratings (id, delivery_id, rated_by, rated_user, score, comment, created_at)
             VALUES (:id, :delivery_id, :rated_by, :rated_user, :score, :comment, :created_at)'
        );
        $stmt->execute([
            'id' => User::generateUuid(),
            'delivery_id' => $delivery['id'],
            'rated_by' => $request->param('auth_user_id'),
            'rated_user' => $input['ratedUser'],
            'score' => $score,
            'comment' => $input['comment'] ?? null,
            'created_at' => time(),
        ]);

        Response::success(null, 'Thanks for your feedback', 201);
    }

    public function complain(Request $request): void
    {
        $input = $request->all();
        Validator::requireFields($input, ['category', 'description']);

        $pdo = Database::connection();
        $stmt = $pdo->prepare(
            'INSERT INTO complaints (id, delivery_id, raised_by, category, description, status, created_at)
             VALUES (:id, :delivery_id, :raised_by, :category, :description, "open", :created_at)'
        );
        $stmt->execute([
            'id' => User::generateUuid(),
            'delivery_id' => $input['deliveryId'] ?? null,
            'raised_by' => $request->param('auth_user_id'),
            'category' => $input['category'],
            'description' => $input['description'],
            'created_at' => time(),
        ]);

        Response::success(null, 'Complaint submitted — our support team will follow up', 201);
    }
}
