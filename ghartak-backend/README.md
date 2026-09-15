# GharTak Backend

PHP REST API for the GharTak local parcel delivery app (Uber Connect–style). Framework-free
by design — a small regex router, PDO with prepared statements, JWT auth, and a consistent
JSON envelope on every response, matching the Android app's `ApiService` 1:1.

## Stack

PHP 8.1+, MySQL/MariaDB, `firebase/php-jwt`, `vlucas/phpdotenv`. No framework — see
`src/Http/Router.php` for why (kept small and auditable rather than pulling in a full stack
for what is currently ~20 routes).

## Project layout

```
public/            Front controller (index.php) + built-in-server router shim
src/
  Config/          Database.php — PDO connection singleton
  Http/            Request, Response, Router, HttpException
  Controllers/     One per resource: Auth, Customer, Delivery, Rider, Payment, RatingComplaint, Health
  Middleware/      AuthMiddleware — JWT verification + role gating
  Models/          User, Delivery, Rider — thin PDO data access, no ORM
  Services/        JwtService, OtpService, PricingService
  Utils/           Validator
routes/api.php     Full route table
database/migrations/001_initial_schema.sql   All tables from spec section 15
```

## Local setup

```bash
composer install
cp .env.example .env        # fill in DB credentials + a real JWT_SECRET
mysql -u root -p ghartak < database/migrations/001_initial_schema.sql
php -S localhost:8080 -t public public/router.php
curl http://localhost:8080/api/health
```

## Deploying to Railway

1. Push this folder to a repo (or a `backend/` subfolder of the monorepo) and connect it in
   Railway.
2. Add a MySQL plugin — Railway injects `DB_HOST`/`DB_PORT`/`DB_DATABASE`/`DB_USERNAME`/
   `DB_PASSWORD` automatically if you name your env vars to match (adjust `Database.php` if
   Railway's MySQL plugin uses different variable names, e.g. `MYSQLHOST` — check the plugin's
   "Variables" tab and either rename in Railway or update `src/Config/Database.php`).
3. Set `JWT_SECRET`, `PRICING_*`, `GOOGLE_MAPS_API_KEY`, `FCM_SERVER_KEY` as Railway environment
   variables. **Never commit real values** — `.env` is gitignored and `.env.example` only has
   placeholders.
4. Railway builds from the `Dockerfile` and runs the app on the `$PORT` it injects.
5. Run the migration against the Railway MySQL instance once (via `railway run mysql ...` or
   a one-off Railway shell) before the API's first real traffic.
6. Confirm `GET /api/health` returns `{"success":true,"data":{"status":"healthy"}}`.

## Auth model

Stateless JWT (`Authorization: Bearer <token>`), issued on login/register, verified by
`AuthMiddleware` on every protected route. Passwords are hashed with `password_hash()`
(bcrypt). No token blocklist yet — `/api/auth/logout` is a no-op placeholder on the server
side; the client just drops its stored token. Add a blocklist table if server-side
revocation becomes a requirement.

## What's implemented vs. stubbed

**Implemented:** registration/login, delivery creation with distance-based pricing
(Haversine), delivery history/detail/cancel, live tracking polling endpoint, OTP-verified
delivery completion, rider online/offline + location updates, nearby-delivery matching for
riders, accept flow, pickup/start/near-destination status transitions, ratings, complaints,
payment record creation + status update, health check.

**Stubbed / next steps:**
- Payment gateway integration — `PaymentController` records intents and status but doesn't
  call any real provider (UPI/Razorpay/etc.) yet, per spec section 12's "design so providers
  can be integrated."
- Push notifications — no FCM sending code yet; `notifications` table exists for when it's
  wired in.
- Chat — `chat_rooms`/`chat_messages` tables exist; no endpoints yet.
- Rider document upload/approval workflow — `rider_documents` table exists; no upload
  endpoint yet.
- Admin endpoints (manage users, pricing, service areas, view all deliveries) — spec section
  2 notes these are backend/admin-API-driven; none built yet.
- Rejection tracking — a rider who rejects a request isn't excluded from seeing it again yet;
  needs a `delivery_rejections` table.
- Rate limiting — spec section 20 asks for it; not implemented at the app layer (add at the
  Railway/reverse-proxy level, or a simple token-bucket table, before production traffic).

## Consistent response envelope

```json
{ "success": true, "message": "Delivery created successfully", "data": { ... } }
{ "success": false, "message": "Invalid or expired OTP", "error_code": "INVALID_OTP" }
```

Every controller throws `App\Http\HttpException` for error cases; `Router::dispatch()`
catches it and produces the error envelope with the right HTTP status — controllers never
call `Response::error()` directly for validation/business-rule failures.
