<?php

declare(strict_types=1);

use App\Controllers\AuthController;
use App\Controllers\CustomerController;
use App\Controllers\DeliveryController;
use App\Controllers\HealthController;
use App\Controllers\PaymentController;
use App\Controllers\RatingComplaintController;
use App\Controllers\RiderController;
use App\Http\Router;
use App\Middleware\AuthMiddleware;

/** @var Router $router */

// --- Health --------------------------------------------------------------
$router->get('/api/health', [new HealthController(), 'check']);

// --- Auth ------------------------------------------------------------------
$router->post('/api/auth/register', [new AuthController(), 'register']);
$router->post('/api/auth/login', [new AuthController(), 'login']);
$router->post('/api/auth/logout', [new AuthController(), 'logout'], [new AuthMiddleware()]);

// --- Customer profile --------------------------------------------------------------
$router->get('/api/customer/profile', [new CustomerController(), 'profile'], [new AuthMiddleware(['customer'])]);
$router->put('/api/customer/profile', [new CustomerController(), 'updateProfile'], [new AuthMiddleware(['customer'])]);

// --- Deliveries (customer-facing) -------------------------------------------------
$deliveryController = new DeliveryController();
$router->post('/api/deliveries', [$deliveryController, 'create'], [new AuthMiddleware(['customer'])]);
$router->get('/api/deliveries', [$deliveryController, 'history'], [new AuthMiddleware(['customer'])]);
$router->get('/api/deliveries/{id}', [$deliveryController, 'show'], [new AuthMiddleware(['customer', 'rider'])]);
$router->post('/api/deliveries/{id}/cancel', [$deliveryController, 'cancel'], [new AuthMiddleware(['customer'])]);
$router->get('/api/deliveries/{id}/tracking', [$deliveryController, 'tracking'], [new AuthMiddleware(['customer', 'rider'])]);
$router->post('/api/deliveries/{id}/verify-otp', [$deliveryController, 'verifyOtp'], [new AuthMiddleware(['rider'])]);

// --- Rider ------------------------------------------------------------------
$riderController = new RiderController();
$router->get('/api/rider/requests', [$riderController, 'requests'], [new AuthMiddleware(['rider'])]);
$router->post('/api/rider/deliveries/{id}/accept', [$riderController, 'accept'], [new AuthMiddleware(['rider'])]);
$router->post('/api/rider/deliveries/{id}/reject', [$riderController, 'reject'], [new AuthMiddleware(['rider'])]);
$router->post('/api/rider/location', [$riderController, 'updateLocation'], [new AuthMiddleware(['rider'])]);
$router->post('/api/rider/status', [$riderController, 'updateStatus'], [new AuthMiddleware(['rider'])]);
$router->post('/api/deliveries/{id}/pickup', [$riderController, 'markPickup'], [new AuthMiddleware(['rider'])]);
$router->post('/api/deliveries/{id}/start', [$riderController, 'startDelivery'], [new AuthMiddleware(['rider'])]);
$router->post('/api/deliveries/{id}/complete', [$riderController, 'complete'], [new AuthMiddleware(['rider'])]);

// --- Payments ------------------------------------------------------------------
$paymentController = new PaymentController();
$router->post('/api/payments/create', [$paymentController, 'create'], [new AuthMiddleware(['customer'])]);
$router->post('/api/payments/verify', [$paymentController, 'verify'], [new AuthMiddleware()]);

// --- Ratings & complaints -------------------------------------------------------
$ratingController = new RatingComplaintController();
$router->post('/api/ratings', [$ratingController, 'rate'], [new AuthMiddleware()]);
$router->post('/api/complaints', [$ratingController, 'complain'], [new AuthMiddleware()]);
