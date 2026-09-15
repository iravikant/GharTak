<?php

declare(strict_types=1);

namespace App\Models;

use App\Config\Database;
use PDO;

final class User
{
    public static function findByPhoneOrEmail(string $identifier): ?array
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare('SELECT * FROM users WHERE phone = :identifier OR email = :identifier LIMIT 1');
        $stmt->execute(['identifier' => $identifier]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function findById(string $id): ?array
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare('SELECT * FROM users WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    public static function phoneExists(string $phone): bool
    {
        $pdo = Database::connection();
        $stmt = $pdo->prepare('SELECT 1 FROM users WHERE phone = :phone LIMIT 1');
        $stmt->execute(['phone' => $phone]);
        return (bool) $stmt->fetchColumn();
    }

    public static function create(
        string $name,
        string $phone,
        ?string $email,
        string $passwordHash,
        string $role
    ): array {
        $pdo = Database::connection();
        $id = self::generateUuid();
        $now = time();

        $pdo->beginTransaction();
        try {
            $stmt = $pdo->prepare(
                'INSERT INTO users (id, name, phone, email, password_hash, role, status, created_at, updated_at)
                 VALUES (:id, :name, :phone, :email, :password_hash, :role, "active", :created_at, :updated_at)'
            );
            $stmt->execute([
                'id' => $id,
                'name' => $name,
                'phone' => $phone,
                'email' => $email,
                'password_hash' => $passwordHash,
                'role' => $role,
                'created_at' => $now,
                'updated_at' => $now,
            ]);

            if ($role === 'customer') {
                $pdo->prepare('INSERT INTO customers (user_id) VALUES (:id)')->execute(['id' => $id]);
            } elseif ($role === 'rider') {
                $pdo->prepare('INSERT INTO riders (user_id, approval_status) VALUES (:id, "pending")')->execute(['id' => $id]);
            }

            $pdo->commit();
        } catch (\Throwable $e) {
            $pdo->rollBack();
            throw $e;
        }

        return self::findById($id);
    }

    public static function generateUuid(): string
    {
        // Avoids pulling in ramsey/uuid as a hard dependency for a single call site;
        // this produces a standard v4 UUID string.
        $data = random_bytes(16);
        $data[6] = chr(ord($data[6]) & 0x0f | 0x40);
        $data[8] = chr(ord($data[8]) & 0x3f | 0x80);
        return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
    }
}
