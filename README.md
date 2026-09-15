# GharTak — Android Scaffold

Customer-side Android scaffold for the local parcel delivery app, built against the full spec
(Uber Connect–style). This is the **foundation layer**: architecture, DI, data layer, and the
first flow (splash → onboarding → auth → home) are wired end-to-end and ready to build on.

## Stack

Kotlin, Jetpack Compose (Material 3), MVVM + Clean Architecture, Hilt, Retrofit + OkHttp
(kotlinx.serialization), Room, DataStore, Coroutines/Flow, Navigation Compose.

## What's implemented

- **Gradle setup** — root + app `build.gradle.kts` with every dependency from the spec
  (Maps Compose, FCM, CameraX, WorkManager, Coil, Accompanist permissions), ProGuard rules,
  manifest with the location/camera/notification permissions the full app will need.
- **Domain layer** (`domain/`) — `User`, `Delivery`, the `DeliveryStatus` state machine
  (spec §7), `ParcelType`, `Address`/`LatLngPoint`, repository interfaces, and use cases
  (`LoginUseCase`, `CreateDeliveryUseCase`, `ObserveDeliveryTrackingUseCase`,
  `VerifyDeliveryOtpUseCase`).
- **Data layer** (`data/`) — `ApiService` (Retrofit interface matching the PHP REST API in
  spec §16), DTOs, Room entities/DAOs for offline delivery-history caching, a DataStore-backed
  `SessionManager` for the auth token, DTO↔domain mappers, and repository implementations.
  `DeliveryRepositoryImpl.observeTracking()` polls `/api/deliveries/{id}/tracking` on the
  cadence from spec §6/§21 and is designed to be swapped for a WebSocket/FCM push later without
  touching any ViewModel.
- **DI** (`di/`) — `NetworkModule` (Retrofit/OkHttp + auth interceptor), `DatabaseModule`
  (Room), `RepositoryModule` (`@Binds`).
- **Presentation** (`presentation/`) — Material 3 theme; reusable components
  (`PrimaryButton`, `LoadingView`, `ErrorView`, `ParcelCard`, `PriceBreakdown`,
  `DeliveryStatusTimeline`); and the first customer flow: Splash → Onboarding → Login/Register →
  Home (active delivery card + recent deliveries), each with its own `ViewModel` exposing
  `StateFlow`.
- **Navigation** (`presentation/navigation/NavGraph.kt`) — routes for the built screens are
  live; routes for the next screens (`CreateDelivery`, `PriceSummary`, `LiveTracking`,
  `DeliveryHistory`, `Profile`) are declared in `Screen.kt` so call sites already compile
  against their eventual destinations.

## Not yet built (next passes)

- Create Delivery flow: pickup/drop map picker, parcel details form, price summary
- Rider matching + live tracking screen (Maps Compose, rider marker, ETA)
- OTP verification + delivery proof capture (CameraX)
- Payment screens
- Chat/call between customer and rider
- Rider-side app (separate flow — dashboard, request cards, navigation, earnings)
- FCM message handling (`notification/ParcelFirebaseMessagingService` is referenced in the
  manifest but not yet implemented)
- Foreground location service for the rider (`location/RiderLocationService`, same — declared,
  not yet implemented)
- The PHP backend, database schema, and Railway deployment config (out of scope for this pass —
  Android app and backend are meant to be developed independently per the spec)

## Running it

This is a source scaffold, not a zipped Gradle wrapper — open the `parcel-delivery-android/`
folder in Android Studio (Koala+ recommended) and let it generate the wrapper, or run
`gradle wrapper` once if you have Gradle installed locally. You'll need to:

1. Add a real `GOOGLE_MAPS_API_KEY` and `BASE_URL` in `app/build.gradle.kts`
   (`buildConfigField`) — placeholders are there so the project doesn't hard-code secrets.
2. Drop in `google-services.json` once you set up Firebase, then uncomment the
   `com.google.gms.google-services` plugin lines in `app/build.gradle.kts`.

## Architecture at a glance

```
UI (Composable) → ViewModel (StateFlow) → UseCase → Repository → Retrofit / Room → PHP API
```

Business logic lives in use cases and repositories, never in Composables — ViewModels only
hold and expose UI state.
