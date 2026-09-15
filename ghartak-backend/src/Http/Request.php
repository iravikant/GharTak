<?php

declare(strict_types=1);

namespace App\Http;

/**
 * Thin wrapper over PHP superglobals so controllers never touch $_SERVER/$_GET directly.
 * Keeps the framework-free router testable without a real HTTP request.
 */
final class Request
{
    private array $body;
    private array $query;
    private array $params = [];
    private array $headers;
    public readonly string $method;
    public readonly string $path;

    public function __construct()
    {
        $this->method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
        $uri = $_SERVER['REQUEST_URI'] ?? '/';
        $this->path = rtrim(parse_url($uri, PHP_URL_PATH) ?: '/', '/') ?: '/';
        $this->query = $_GET ?? [];
        $this->headers = self::extractHeaders();

        $raw = file_get_contents('php://input') ?: '';
        $decoded = json_decode($raw, true);
        $this->body = is_array($decoded) ? $decoded : [];
    }

    private static function extractHeaders(): array
    {
        $headers = [];
        foreach ($_SERVER as $key => $value) {
            if (str_starts_with($key, 'HTTP_')) {
                $name = str_replace('_', '-', substr($key, 5));
                $headers[strtolower($name)] = $value;
            }
        }
        if (isset($_SERVER['CONTENT_TYPE'])) {
            $headers['content-type'] = $_SERVER['CONTENT_TYPE'];
        }
        return $headers;
    }

    public function setParams(array $params): void
    {
        $this->params = $params;
    }

    public function mergeParams(array $params): void
    {
        $this->params = array_merge($this->params, $params);
    }

    public function param(string $key, mixed $default = null): mixed
    {
        return $this->params[$key] ?? $default;
    }

    public function input(string $key, mixed $default = null): mixed
    {
        return $this->body[$key] ?? $default;
    }

    public function all(): array
    {
        return $this->body;
    }

    public function query(string $key, mixed $default = null): mixed
    {
        return $this->query[$key] ?? $default;
    }

    public function header(string $key, mixed $default = null): mixed
    {
        return $this->headers[strtolower($key)] ?? $default;
    }

    public function bearerToken(): ?string
    {
        $auth = $this->header('authorization', '');
        if (is_string($auth) && str_starts_with($auth, 'Bearer ')) {
            return substr($auth, 7);
        }
        return null;
    }
}
