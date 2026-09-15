<?php

declare(strict_types=1);

namespace App\Models;

use App\Config\Database;

final class Delivery
{
    public static function create(array $data): array
    {
        $pdo = Database::connection();
        $id = User::generateUuid();
        $now = time();

        $stmt = $pdo->prepare(
            'INSERT INTO deliveries (
                id, customer_id, pickup_lat, pickup_lng, pickup_address,
                drop_lat, drop_lng, drop_address, receiver_name, receiver_phone,
                parcel_type, parcel_weight, parcel_description, parcel_image_url,
                delivery_fee, platform_fee, total_amount,
                payment_status, delivery_status, created_at
            ) VALUES (
                :id, :customer_id, :pickup_lat, :pickup_lng, :pickup_address,
                :drop_lat, :drop_lng, :drop_address, :receiver_name, :receiver_phone,
                :parcel_type, :parcel_weight, :parcel_description, :parcel_image_url,
                :delivery_fee, :platform_fee, :total_amount,
                "PENDING", "SEARCHING_RIDER", :created_at
            )'
        );

        $stmt->execute([
            'id' => $id,
            'customer_id' => $data['customerId'],
            'pickup_lat' => $data['pickup']['lat'],
            'pickup_lng' => $data['pickup']['lng'],
            'pickup_address' => $data['pickup']['formattedAddress'],
            'drop_lat' => $data['drop']['lat'],
            'drop_lng' => $data['drop']['lng'],
            'drop_address' => $data['drop']['formattedAddress'],
            'receiver_name' => $data['receiverName'],
            'receiver_phone' => $data['receiverPhone'],
            'parcel_type' => $data['parcelType'],
            'parcel_weight' => $data['parcelWeightKg'],
            'parcel_description' => $data['parcelDescription'],
            'parcel_image_url' => $data['parcelImageUrl'],
            'delivery_fee' => $data['deliveryFee'],
            'platform_fee' => $data['platformFee'],
            'total_amount' => $data['totalAmount'],
            'created_at' => $now,
        ]);

        self::recordStatusHistory($id, 'SEARCHING_RIDER', $data['customerId']);

        return self::findById($id);
    }

    public static function findById(string $id): ?array
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare('SELECT * FROM deliveries WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function belongsToCustomer(string $deliveryId, string $customerId): bool
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare('SELECT 1 FROM deliveries WHERE id = :id AND customer_id = :customer_id LIMIT 1');
        $stmt->execute(['id' => $deliveryId, 'customer_id' => $customerId]);
        return (bool) $stmt->fetchColumn();
    }

    public static function historyForCustomer(string $customerId, int $page, int $pageSize): array
    {
        $pdo = Database::connection();
        $offset = max(0, ($page - 1) * $pageSize);
        $stmt = $pdo->prepare(
            'SELECT * FROM deliveries WHERE customer_id = :customer_id
             ORDER BY created_at DESC LIMIT :limit OFFSET :offset'
        );
        $stmt->bindValue('customer_id', $customerId);
        $stmt->bindValue('limit', $pageSize, \PDO::PARAM_INT);
        $stmt->bindValue('offset', $offset, \PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll();
    }

    public static function updateStatus(string $id, string $status, ?string $changedBy = null): void
    {
        $pdo = Database::connection();
        $timestampColumn = match ($status) {
            'RIDER_ASSIGNED' => 'accepted_at',
            'PARCEL_PICKED_UP' => 'picked_up_at',
            'DELIVERED' => 'delivered_at',
            'CANCELLED' => 'cancelled_at',
            default => null,
        };

        $sql = 'UPDATE deliveries SET delivery_status = :status';
        if ($timestampColumn !== null) {
            $sql .= ", {$timestampColumn} = :ts";
        }
        $sql .= ' WHERE id = :id';

        $stmt = $pdo->prepare($sql);
        $params = ['status' => $status, 'id' => $id];
        if ($timestampColumn !== null) {
            $params['ts'] = time();
        }
        $stmt->execute($params);

        self::recordStatusHistory($id, $status, $changedBy);
    }

    public static function assignRider(string $deliveryId, string $riderId): void
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare('UPDATE deliveries SET rider_id = :rider_id WHERE id = :id');
        $stmt->execute(['rider_id' => $riderId, 'id' => $deliveryId]);
        self::updateStatus($deliveryId, 'RIDER_ASSIGNED', $riderId);
    }

    public static function setOtp(string $deliveryId, string $otp, int $expiresAt): void
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare('UPDATE deliveries SET otp = :otp, otp_expires_at = :exp WHERE id = :id');
        $stmt->execute(['otp' => $otp, 'exp' => $expiresAt, 'id' => $deliveryId]);
    }

    public static function recordStatusHistory(string $deliveryId, string $status, ?string $changedBy): void
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare(
            'INSERT INTO delivery_status_history (id, delivery_id, status, changed_at, changed_by)
             VALUES (:id, :delivery_id, :status, :changed_at, :changed_by)'
        );
        $stmt->execute([
            'id' => User::generateUuid(),
            'delivery_id' => $deliveryId,
            'status' => $status,
            'changed_at' => time(),
            'changed_by' => $changedBy,
        ]);
    }

    public static function latestRiderLocation(string $riderId): ?array
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare('SELECT * FROM rider_locations WHERE rider_id = :rider_id LIMIT 1');
        $stmt->execute(['rider_id' => $riderId]);
        $row = $stmt->fetch();
        return $row ?: null;
    }
}
