# GharTak — Android Scaffold

Customer-side Android scaffold for a local parcel delivery app (Uber Connect–style). Data layer
runs entirely on **Firebase** — Firestore, Firebase Auth (Phone OTP), and Firebase Storage —
there is no separate backend server to deploy or maintain.

## Stack

Kotlin, Jetpack Compose (Material 3), MVVM + Clean Architecture, Hilt, **Firestore + Firebase
Auth + Firebase Storage**, Coroutines/Flow, Navigation Compose.

## Why Firestore instead of a REST backend

- **Real-time by default.** `DeliveryRepositoryImpl.observeTracking()` attaches Firestore
  snapshot listeners to the delivery doc and the assigned rider's live-location doc. Updates
  push to the customer's screen the instant the rider's app writes a new location — no polling
  interval, no "stale for up to N seconds" trade-off a REST poll loop would have.
- **Offline for free.** Firestore's persistent local cache (enabled in `FirebaseModule`) serves
  reads and queues writes while offline, replacing what would otherwise be a hand-rolled Room
  cache layer.
- **No server to operate.** Auth, data storage, and file storage are all managed services —
  there's nothing to deploy to Railway, patch, or scale.

## What's implemented

- **Gradle setup** — Firebase BOM (Firestore, Auth, Storage, Messaging, Crashlytics, Analytics,
  App Check), Maps Compose, CameraX, WorkManager, Coil, Accompanist permissions. ProGuard rules
  and manifest permissions for location/camera/notifications.
- **Domain layer** (`domain/`) — `User`, `Delivery`, the `DeliveryStatus` state machine
  (spec §7), `ParcelType`, `Address`/`LatLngPoint`, `OtpRequestState`, repository interfaces, and
  use cases (`RequestOtpUseCase`, `VerifyLoginOtpUseCase`, `CreateDeliveryUseCase`,
  `ObserveDeliveryTrackingUseCase`, `VerifyDeliveryOtpUseCase`).
- **Data layer** (`data/`)
  - `data/firebase/FirestoreSchema.kt` — single source of truth for every collection and field
    name, kept in sync with `firebase/firestore.rules` and `firebase/firestore.indexes.json`.
  - `data/firebase/FirebaseErrorMapper.kt` — turns raw Firebase exceptions (network, rate
    limit, permission-denied, invalid OTP, etc.) into the same `Result.Error` every repository
    already returns, so screens never see SDK-specific error text.
  - `data/model/Mappers.kt` — hand-written `DocumentSnapshot` ↔ domain conversions. Each field
    has an explicit fallback so a partially-written or malformed document degrades gracefully
    instead of throwing during deserialization.
  - `AuthRepositoryImpl` — Firebase Phone Auth: sends an OTP, handles Android's automatic SMS
    retrieval, and on successful verification reads (or creates, for a new number) the user's
    `users/{uid}` Firestore profile.
  - `DeliveryRepositoryImpl` — Firestore CRUD for deliveries, a transaction-based OTP
    verification (atomic, race-safe), cursor-paginated history, and image upload to Firebase
    Storage.
  - `SessionManager` — DataStore-backed cache of the signed-in user's profile only (name, role,
    photo), so the app can render instantly on launch without a Firestore round trip. Firebase
    Auth itself remains the sole source of truth for *whether* someone is signed in.
- **DI** (`di/`) — `FirebaseModule` (Firestore with persistent cache, Auth, Storage),
  `RepositoryModule` (`@Binds`).
- **Presentation** (`presentation/`) — Material 3 theme; reusable components
  (`PrimaryButton`, `LoadingView`, `ErrorView`, `ParcelCard`, `PriceBreakdown`,
  `DeliveryStatusTimeline`); and the first customer flow: Splash → Onboarding → **phone + OTP
  sign-in (`AuthScreen`)** → Home (active delivery card + recent deliveries), each with its own
  `ViewModel` exposing `StateFlow`.
- **Navigation** (`presentation/navigation/NavGraph.kt`) — routes for the built screens are
  live; routes for the next screens (`CreateDelivery`, `PriceSummary`, `LiveTracking`,
  `DeliveryHistory`, `Profile`) are declared in `Screen.kt` so call sites already compile
  against their eventual destinations.
- **`/firebase/`** — deployable security config, separate from the Android app:
  - `firestore.rules` — per-collection access control (users can only write their own profile;
    a delivery is only readable/writable by its customer or assigned rider; the OTP field is
    immutable after creation; chat messages are scoped to the two parties on that delivery).
  - `storage.rules` — parcel/delivery-proof photo uploads are capped at 8MB, image-only, and
    path-scoped so only the uploader can write to their own folder.
  - `firestore.indexes.json` — composite indexes for the customer-history query
    (`customerId` + `createdAt`) and two more anticipated for the rider app
    (`riderId` + `createdAt`, `status` + `createdAt`).
  - `firebase.json` — ties the above together for the Firebase CLI.

## Not yet built (next passes)

- Create Delivery flow: pickup/drop map picker, parcel details form, price summary
- Rider matching + live tracking screen (Maps Compose, rider marker, ETA) — the repository
  layer (`observeTracking`) is ready; this is the UI that consumes it
- Delivery proof capture (CameraX) wired to `uploadParcelImage`
- Payment screens
- In-app chat between customer and rider (`chatRooms/{deliveryId}/messages` rules are already
  written; no UI yet)
- Rider-side app (separate flow — dashboard, request cards, navigation, earnings)
- FCM message handling (`notification/ParcelFirebaseMessagingService` is referenced in the
  manifest but not yet implemented)
- Foreground location service for the rider (`location/RiderLocationService`, same — declared,
  not yet implemented)

## Security note: delivery OTP

`verifyDeliveryOtp` runs as a Firestore transaction, which prevents a race between two
concurrent writes — but the raw OTP still has to be *readable* by the rider's client to compare
it, since there's no server to check it on the client's behalf. `firestore.rules` restricts who
can read the delivery doc at all (customer + assigned rider only) and makes the `otp` field
immutable after creation, which is a reasonable bar for a client-only architecture. For a
production launch, move this specific check into a **Firestore-triggered Cloud Function**
(callable, with the OTP never sent to the client at all) — the rest of the app's architecture
doesn't need to change to adopt that later.

## Running it

1. Create a Firebase project, add an Android app with package name `com.ghartak.app`, and
   download the real `google-services.json` over the placeholder one in `app/`.
2. Enable **Phone** sign-in under Firebase Auth → Sign-in method, and add your test/SHA-1
   fingerprints if testing on a real device (required for the SMS auto-retrieval + reCAPTCHA
   fallback to work).
3. Deploy the security rules and indexes: `firebase deploy --only firestore:rules,firestore:indexes,storage` (from the project root, with the Firebase CLI installed and logged in).
4. Add a real `GOOGLE_MAPS_API_KEY` in `app/build.gradle.kts` (`buildConfigField`).
5. Open the project in Android Studio (Koala+ recommended) and run.

## Architecture at a glance

```
UI (Composable) → ViewModel (StateFlow) → UseCase → Repository → Firestore / Firebase Auth / Storage
```

Business logic lives in use cases and repositories, never in Composables — ViewModels only hold
and expose UI state. Real-time data (delivery status, rider location) flows from Firestore
listeners through the repository as a `Flow`, straight into a `StateFlow` the UI collects — no
manual refresh or polling anywhere in the stack.


---

## Authentication (updated)

Three ways to log in, one way to sign up — all backed by Firebase Auth + a Firestore `users/{uid}` profile:

| Flow | What it does |
|------|--------------|
| **Log in → Email** | Email + password, with "Forgot password?" (sends a reset email) |
| **Log in → Mobile OTP** | +91 mobile number → 6-digit SMS code (auto-submits on the 6th digit, resend after 30 s) |
| **Continue with Google** | Credential Manager account picker → Firebase credential |
| **Sign up** | Name, mobile, email, password (strength meter) → then an optional "verify your mobile" OTP step that **links** the phone to the same account, so mobile-OTP login later opens the *same* account |

Code map: `AuthScreen` / `AuthViewModel` (presentation), `SignInWithEmailUseCase`, `SignUpWithEmailUseCase`,
`SignInWithGoogleUseCase`, `SendPasswordResetUseCase`, `RequestOtpUseCase`, `VerifyOtpUseCase` (domain),
`AuthRepositoryImpl` + `GoogleSignInHelper` + `AuthErrorMapper` (data).

### Firebase console checklist (do all of these)

1. **Authentication → Sign-in method**: enable **Email/Password**, **Phone** and **Google**.
2. **Project settings → Your apps → Android**: add the **SHA-1 *and* SHA-256** of the keystore you are running with
   (`./gradlew signingReport`), then **re-download `google-services.json`**. Each machine / release key needs its own.
3. **Authentication → Settings → SMS region policy**: make sure **India (+91)** is allowed.
4. Real SMS can require the **Blaze (billing)** plan — the app shows `BILLING_NOT_ENABLED` if that is the cause.
5. If **App Check** enforcement is switched on for Authentication, register a debug token or turn enforcement off while developing.

### "OTP not received" — how to diagnose

* The app now shows the **actual reason** instead of a generic failure (missing SHA fingerprint, blocked SMS region,
  billing, reCAPTCHA, invalid number, rate limit …). Full exception is logged under Logcat tag **`GharTakAuth`**.
* **Test without SMS:** Firebase console → Authentication → Sign-in method → Phone → *Phone numbers for testing* →
  add e.g. `+91 99999 99999` with code `123456`. It works instantly, costs nothing and bypasses carrier delivery.
* Use a real device or an emulator image **with Google Play services**.

### Housekeeping note

The legacy REST/Room files (`data/remote`, `data/local/dao|entity`, `AppDatabase`, `NetworkModule`, `DatabaseModule`)
and the Retrofit/Room dependencies from the original PHP plan are still in the project but unused. They are safe
to delete (together with those Gradle dependencies and the `BASE_URL` build field) whenever you like.

## Design system

Brand indigo→violet gradient, rounded 16–28 dp shapes, dynamic colour intentionally off for a consistent identity,
full light/dark support. Reusable pieces live in `presentation/components` (`PrimaryButton`, `GoogleButton`,
`GharTakTextField`, `OtpInput`, `SegmentedToggle`, `InfoBanner`, `ParcelCard`, `EmptyState`, `SkeletonBox`,
`MainScaffold` bottom navigation, `Avatar`, `StatusPill`). Screens: Splash, Onboarding (shown once), Auth, Home,
Orders, Profile. Flows not built yet (booking, live tracking, notifications, saved addresses, help) open a
"Coming soon" screen instead of crashing.


---

## Booking, addresses, profile, services (latest)

| Feature | Where | Notes |
|---|---|---|
| **Send a parcel** (3 steps) | `presentation/booking` | Route → Parcel details → Review & book. One `BookingViewModel` shared by all steps (scoped to the `booking` nav graph), so Back never loses input. |
| **Location pickup** | `presentation/location`, `location/` | Full-screen map with centre pin, address search (platform Geocoder), "use my location" (Fused Location), runtime permission handling. |
| **Fare + ETA** | `domain/model/PricingRules`, `CalculatePriceUseCase` | Base fare + per-km + weight surcharge + ₹10 platform fee. Distance is straight-line × 1.3 (no Directions API). 40 km range limit. |
| **Delivery receipt / tracking status** | `presentation/delivery` | Live (Firestore snapshot listener): status hero, delivery OTP card, progress timeline, route, parcel, payment, cancel. |
| **Saved addresses** | `presentation/addresses` | Home / Office / Other, stored at `users/{uid}/savedAddresses`; usable as pickup or drop in booking. |
| **Edit profile** | `presentation/profile/EditProfile*` | Name + profile photo (Firebase Storage `users/{uid}/profile/…`). Email and mobile are read-only (account identifiers). |
| **Services** | `presentation/services` | All parcel types with description, weight limit and "from ₹" price; tapping one starts booking with that type preselected. |

### Setup needed for the map
1. In Google Cloud console enable **Maps SDK for Android** for your project and create an API key
   (restrict it to package `com.ghartak.app` + your SHA-1).
2. Put the key in **both** places in `app/build.gradle.kts`: `buildConfigField("String", "MAPS_API_KEY", ...)` and
   `manifestPlaceholders["MAPS_API_KEY"]`.
3. Without a key the map shows blank tiles, but search + "use my location" + confirm still work.

### Known limitations (by design for now)
* **Pricing and the delivery OTP are generated on the client.** Move both into a Cloud Function before launch.
* **Payment:** only cash on delivery is selectable; UPI / card show "Coming soon".
* **No rider app yet**, so a new booking stays at "Finding a rider" until a rider accepts it (rider app / console edit).
* Address search uses the platform Geocoder; swap in Google Places Autocomplete for richer suggestions.


---

## Rider mode (find & deliver)

There is no separate rider app: the same GharTak app has a **Rider mode** (Profile → *Rider mode · deliver & earn*).
A user becomes a rider by registering a vehicle; an admin approves them; then they go online and accept jobs.

### How a customer finds a rider
1. Customer books → app writes the delivery **and** a minimal `deliveryRequests/{id}` offer in one batch
   (coarse pickup/drop areas, ~100 m rounded pickup coordinates, parcel type, weight, distance, rider earnings —
   *no* receiver name/phone or exact addresses).
2. Every **online, approved** rider sees offers within **15 km** of their GPS position, nearest first, updating live
   (Firestore snapshot listener).
3. A rider taps **Accept** → one Firestore **transaction** claims the offer and assigns the delivery
   (`riderId`, status `RIDER_ASSIGNED`). If two riders tap at once, the second gets "Another rider just took this delivery."
4. The customer's screen flips instantly: rider card (name, vehicle, call button), live map with the rider's marker,
   and distance to pickup/drop. The rider only now gets read access to the full delivery (addresses, receiver phone).

### Rider flow
Register vehicle → **pending** → admin approves → dashboard (online switch, today's earnings, requests) → accept →
*Start trip → Reached pickup → Parcel picked up → Start delivery → enter the customer's 4-digit code → Delivered*.
Navigation and call buttons open Google Maps / the dialer. Cash-on-delivery amount is shown to collect.

### Approving a rider (admin step)
Firebase console → Firestore → `riders/{uid}` → set `approvalStatus` to `APPROVED`. The rider's screen updates by itself.
*Quick dev shortcut:* set `Constants.AUTO_APPROVE_RIDERS = true` and, in `firebase/firestore.rules`, change the riders
`create` rule to `in ['PENDING','APPROVED']` (revert before launch).

### Deploy the rules and the new index (required)
`firebase deploy --only firestore:rules,firestore:indexes` — the offers list needs the `deliveryRequests`
(status + createdAt) index, and riders/offers/claims are enforced by the new rules (field-level: a rider can't
touch fees or the OTP; a customer can only cancel before pickup).

### Testing end-to-end with one phone
Use two accounts (two devices, or log out/in): account A books a parcel in customer mode; account B registers as a rider,
you approve it in the console, B goes online near A's pickup and accepts. (A rider never sees their own bookings.)

### Limits to know about
* Location sharing runs while the app is open; a **foreground service** is needed to keep it alive with the screen off.
* The delivery OTP check and fares are still client-side — move them to Cloud Functions before launch.
* No push notifications yet (riders see offers live only while the screen is open) — FCM is the next step.
* No ratings yet, so the rider card shows no star score until ratings exist.

## Profile: changing the mobile number
Edit profile → *Mobile number → Change* → new number → 6-digit code → verified and swapped on the account
(`updatePhoneNumber`, or linked if no phone was attached). Firebase requires a recent sign-in for this; if it asks,
log out and back in and retry.
