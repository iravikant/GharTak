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

/**
 * Payment provider integration is intentionally not wired in yet (spec section 12 asks for
 * a backend "designed so payment providers can be integrated" — not a specific gateway).
 * These endpoints record the payment intent and status transitions; swap the body of
 * verify() for a real gateway webhook/callback handler once a provider is chosen.
 */
final class PaymentController
{
    public function create(Request $request): void
    {
        $input = $request->all();
        Validator::requireFields($input, ['deliveryId', 'method']);

        $delivery = Delivery::findById((string) $input['deliveryId']);
        if ($delivery === null) {
            throw new HttpException('Delivery not found', 'NOT_FOUND', 404);
        }

        $method = strtoupper((string) $input['method']);
        if (!in_array($method, ['COD', 'UPI', 'ONLINE', 'WALLET'], true)) {
            throw new HttpException('Unsupported payment method', 'VALIDATION_ERROR', 422);
        }

        $pdo = Database::connection();
        $id = User::generateUuid();
        $now = time();
        $status = $method === 'COD' ? 'PENDING' : 'PROCESSING';

        $stmt = $pdo->prepare(
            'INSERT INTO payments (id, delivery_id, amount, method, status, created_at, updated_at)
             VALUES (:id, :delivery_id, :amount, :method, :status, :created_at, :updated_at)'
        );
        $stmt->execute([
            'id' => $id,
            'delivery_id' => $delivery['id'],
            'amount' => $delivery['total_amount'],
            'method' => $method,
            'status' => $status,
            'created_at' => $now,
            'updated_at' => $now,
        ]);

        Response::success(['paymentId' => $id, 'status' => $status], 'Payment initiated', 201);
    }

    public function verify(Request $request): void
    {
        $input = $request->all();
        Validator::requireFields($input, ['paymentId', 'status']);

        $status = strtoupper((string) $input['status']);
        if (!in_array($status, ['SUCCESS', 'FAILED', 'REFUNDED'], true)) {
            throw new HttpException('Invalid payment status', 'VALIDATION_ERROR', 422);
        }

        $pdo = Database::connection();
        $stmt = $pdo->prepare('UPDATE payments SET status = :status, updated_at = :updated_at WHERE id = :id');
        $stmt->execute(['status' => $status, 'updated_at' => time(), 'id' => $input['paymentId']]);

        Response::success(null, 'Payment status updated');
    }
}
