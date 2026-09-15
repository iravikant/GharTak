# Keep Retrofit/OkHttp/Kotlin Serialization models
-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep,includedescriptorclasses class com.ghartak.app.data.remote.dto.**$$serializer { *; }
-keepclassmembers class com.ghartak.app.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.ghartak.app.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
