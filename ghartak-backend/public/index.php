<?php

declare(strict_types=1);

require __DIR__ . '/../vendor/autoload.php';

use App\Http\Request;
use App\Http\Router;

// Load .env locally; on Railway, environment variables are injected directly and this
// silently no-ops if no .env file is present.
if (file_exists(__DIR__ . '/../.env')) {
    $dotenv = Dotenv\Dotenv::createImmutable(__DIR__ . '/..');
    $dotenv->load();
}

error_reporting(E_ALL);
ini_set('display_errors', ($_ENV['APP_DEBUG'] ?? 'false') === 'true' ? '1' : '0');

// CORS — the Android app calls this over HTTPS with a bearer token, not cookies, so a
// permissive origin here doesn't expose session-based attacks; tighten to specific origins
// if a web admin dashboard is added later.
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$router = new Router();
require __DIR__ . '/../routes/api.php';

$request = new Request();
$router->dispatch($request);
