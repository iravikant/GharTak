# Ye Lo !! — local parcel delivery

Send a parcel across the city and watch it arrive. **Ye Lo !!** is an Android app (Kotlin, Jetpack Compose, Firebase)
with two modes in one app: **Customer** (book, track, rate) and **Rider** (go online, accept jobs, deliver, earn).

> **Naming note.** The app's display name is **Ye Lo !!**. The internal Android package / `applicationId` is still
> `com.ghartak.app` on purpose: changing it means registering a new Android app in Firebase and replacing
> `google-services.json` (and re-adding SHA fingerprints). Users never see it; rename it later only if you want to.

---

## Features

**Customer**
- Sign up (name, mobile, email, password) · log in with **email + password**, **mobile OTP**, or **Google** · forgot-password email
- Optional mobile verification after sign-up (links the phone so OTP login opens the *same* account)
- **Send a parcel** in 3 steps: pickup/drop on a map (search, current location, saved places) → parcel details
  (type, weight, receiver, photo) → fare + payment → book
- Fare + ETA before you confirm (base + per-km + weight surcharge + platform fee; 40 km range)
- **Live delivery screen**: status timeline, delivery OTP to share with the receiver, assigned rider (name, vehicle,
  rating, call button), live map with the rider's position, cancel (until pickup)
- **Rate the rider** after delivery (1–5 stars + comment) · rider average shown to future customers
- Orders history (paginated) · Notifications feed · Saved addresses (Home/Office/Other) · Services catalogue
- **Edit profile**: name, photo, and **mobile number** (OTP-verified change)
- **Help & support**: FAQ, report a problem (stored for admins), your reports, email support

**Rider** (Profile → *Rider mode*)
- Register vehicle → admin approval → dashboard with **online/offline** switch, today's earnings, deliveries, rating
- Live **job offers** within 15 km, nearest first, with the rider's earnings; first rider to accept wins (atomic)
- Job screen: stage buttons (start trip → at pickup → picked up → start delivery), Google Maps navigation, call
  sender/receiver, cash-on-delivery reminder, **OTP handover** to complete

**Quality**
- Material 3 design system, light/dark, branded adaptive icon, skeleton loading, empty/error states everywhere
- Offline-friendly (Firestore cache), clear error messages (auth, OTP delivery diagnostics, Firestore, Storage)
- Firestore/Storage **security rules** with field-level restrictions, unit tests for fare/validation logic

---

## Tech & architecture

Kotlin · Jetpack Compose (Material 3) · MVVM + Clean Architecture · Hilt · Navigation Compose · Coroutines/Flow ·
Firebase Auth / Firestore / Storage / Crashlytics / Analytics · Google Maps Compose · Fused Location · Coil · DataStore

```
UI (Composable) → ViewModel (StateFlow) → UseCase → Repository (interface) → Firestore / Auth / Storage
```

```
app/src/main/java/com/ghartak/app
  data/        firebase error mappers, mappers, repository implementations, local session cache
  domain/      models, repository interfaces, use cases, pricing rules
  presentation/ auth · booking · delivery · home · orders · profile · addresses · services · help
                notifications · rider · location · onboarding · splash · components · theme · navigation
  location/    fused-location provider, geocoder, rider location tracker
  di/          Hilt modules          util/ constants, validators, result type
firebase/      firestore.rules · storage.rules · firestore.indexes.json   (deployed with the Firebase CLI)
```

Key collections: `users`, `users/{uid}/savedAddresses`, `deliveries`, `deliveryRequests` (rider-visible offers),
`riders`, `riderLocations`, `ratings`, `complaints`.

---

## One-time setup (do all of these)

1. **Firebase project** `ghartak-577cb` is already wired in `.firebaserc` / `google-services.json`. If you use a different
   project, replace `google-services.json` (package `com.ghartak.app`) and `.firebaserc`.
2. **Authentication → Sign-in method**: enable **Email/Password**, **Phone** and **Google**.
3. **Project settings → Android app**: add the **SHA-1 and SHA-256** of every keystore you run with
   (`./gradlew signingReport`), then re-download `google-services.json`.
4. **Firestore Database**: create it (production mode). **Storage**: *Get started* (new projects may require the Blaze plan);
   make sure the bucket in the console equals `storage_bucket` in `google-services.json`.
5. **Deploy rules & indexes** (required — booking, riders, ratings and reports depend on them):
   ```
   firebase deploy --only firestore:rules,firestore:indexes,storage
   ```
6. **Google Maps**: enable *Maps SDK for Android*, create a key restricted to `com.ghartak.app` + your SHA-1, and put it in
   **both** places in `app/build.gradle.kts` (`buildConfigField MAPS_API_KEY` and `manifestPlaceholders["MAPS_API_KEY"]`).
7. Replace `Constants.SUPPORT_EMAIL` with your real support address, and review the placeholder rates in
   `domain/model/PricingRules.kt` (fares, platform fee, rider share).
8. Open in Android Studio (Koala+), sync, run on a device or an emulator with Google Play services.

### Trying it end-to-end (two accounts)
1. Account **A** (customer): sign up → *Send a parcel* → book.
2. Account **B**: sign up → Profile → *Rider mode* → register a vehicle.
3. In the Firebase console set `riders/{B's uid}.approvalStatus` to `APPROVED` (the rider screen updates by itself).
4. B goes **online** near A's pickup → sees the offer → **Accept**. A's screen now shows B, the live map and the delivery code.
5. B walks through the stages and enters A's 4-digit code → *Delivered*. A can rate B.

*Dev shortcut to skip the approval step:* `Constants.AUTO_APPROVE_RIDERS = true` **and** change the `riders` `create` rule in
`firebase/firestore.rules` to `in ['PENDING','APPROVED']`. Revert both before launch.

### Test without sending SMS
Firebase console → Authentication → Sign-in method → Phone → **Phone numbers for testing** → add e.g. `+91 99999 99999` = `123456`.

---

## Tests
```
./gradlew testDebugUnitTest
```
Covers fare calculation, weight surcharges, range/limits, rider earnings, Haversine distance, input validation and address
anonymising (`app/src/test`).

---

## Troubleshooting
| Symptom | Cause / fix |
|---|---|
| OTP never arrives | Phone sign-in off, missing SHA-1/256, SMS region (India) blocked, or billing. The app shows the real reason; full exception under Logcat tag **`YeLoAuth`**. Use a test number (above). |
| `PERMISSION_DENIED` when booking / registering as rider / rating | The latest `firestore.rules` aren't deployed (step 5). |
| Photo upload `404 Object does not exist` | Storage isn't set up, or the bucket name differs from `google-services.json` (step 4). Booking still works without a photo. |
| "query requires an index" | Deploy `firestore.indexes.json` (step 5). |
| Map is blank | Maps API key missing/unrestricted for this build (step 6). Search + "use my location" still work. |

---

## Before you launch (known limitations)
- **Fares and the delivery OTP are generated on the phone.** Move both to Cloud Functions so a modified app can't underpay or read the code.
- **Payments**: cash on delivery only; UPI / cards are shown as "Coming soon".
- **Push notifications (FCM)** are not implemented — riders see offers live only while the app is open, and the Notifications
  screen is an in-app feed of delivery updates. Add FCM + a Cloud Function sender next.
- **Rider location** is shared while the app is in the foreground; keeping it alive with the screen off needs a **foreground service**.
- Rider approval is a manual console step; build a small admin panel when volume grows.
- Rider ratings are averaged on read (last 100); aggregate server-side at scale.
- Enable **App Check** (Play Integrity) and set a release signing config; release builds already use R8/ProGuard.
