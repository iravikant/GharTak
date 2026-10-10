# Firebase / Firestore — the SDK ships its own consumer ProGuard rules, these just cover the
# classes we construct reflectively or expect in stack traces.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }

# Keep domain model classes used as Firestore document fields readable in Crashlytics stack traces.
-keep class com.ghartak.app.domain.model.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Crashlytics — keep file/line number info for de-obfuscated stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
