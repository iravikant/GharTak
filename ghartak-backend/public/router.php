<?php
// Used only with `php -S` (see Dockerfile). Apache/Nginx setups should point their
// document root at public/ with a standard rewrite-to-index rule instead of this file.

$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$filePath = __DIR__ . $path;

if ($path !== '/' && file_exists($filePath) && !is_dir($filePath)) {
    return false; // let the built-in server serve the static file as-is
}

require __DIR__ . '/index.php';
