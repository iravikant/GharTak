<?php

declare(strict_types=1);

namespace App\Http;

/**
 * Small dependency-free router: register(method, path, handler, middlewares[]).
 * Path params use {name} syntax, e.g. /api/deliveries/{id}.
 *
 * Kept intentionally minimal rather than pulling in a full framework — the spec
 * calls for a lean PHP backend, and the route table (routes/api.php) is small
 * enough that a regex dispatcher is easier to audit than a framework's routing cache.
 */
final class Router
{
    /** @var array<int, array{method:string, pattern:string, paramNames:array, handler:callable, middlewares:array}> */
    private array $routes = [];

    public function get(string $path, callable $handler, array $middlewares = []): void
    {
        $this->add('GET', $path, $handler, $middlewares);
    }

    public function post(string $path, callable $handler, array $middlewares = []): void
    {
        $this->add('POST', $path, $handler, $middlewares);
    }

    public function put(string $path, callable $handler, array $middlewares = []): void
    {
        $this->add('PUT', $path, $handler, $middlewares);
    }

    public function delete(string $path, callable $handler, array $middlewares = []): void
    {
        $this->add('DELETE', $path, $handler, $middlewares);
    }

    private function add(string $method, string $path, callable $handler, array $middlewares): void
    {
        $paramNames = [];
        $pattern = preg_replace_callback('/\{(\w+)\}/', function ($m) use (&$paramNames) {
            $paramNames[] = $m[1];
            return '([^/]+)';
        }, $path);

        $this->routes[] = [
            'method' => $method,
            'pattern' => '#^' . rtrim($pattern, '/') . '$#',
            'paramNames' => $paramNames,
            'handler' => $handler,
            'middlewares' => $middlewares,
        ];
    }

    public function dispatch(Request $request): void
    {
        $path = $request->path === '' ? '/' : $request->path;

        foreach ($this->routes as $route) {
            if ($route['method'] !== $request->method) {
                continue;
            }
            if (preg_match($route['pattern'], $path, $matches)) {
                array_shift($matches);
                $params = array_combine($route['paramNames'], $matches) ?: [];
                $request->setParams($params);

                try {
                    foreach ($route['middlewares'] as $middleware) {
                        $middleware($request);
                    }
                    ($route['handler'])($request);
                } catch (\App\Http\HttpException $e) {
                    Response::error($e->getMessage(), $e->errorCode, $e->getCode());
                } catch (\Throwable $e) {
                    error_log($e->getMessage());
                    Response::error('Something went wrong. Please try again.', 'INTERNAL_ERROR', 500);
                }
                return;
            }
        }

        Response::error('Route not found', 'NOT_FOUND', 404);
    }
}
