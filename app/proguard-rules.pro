# ProGuard / R8 Rules for KryptUp

# Web3j & Cryptography
-keep class org.web3j.** { *; }
-dontwarn org.web3j.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Firebase & Google Play Services
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Data Models
-keep class com.kryptup.kryptup.data.model.** { *; }
-keepclassmembers class com.kryptup.kryptup.data.model.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Jetpack Biometrics & CameraX
-dontwarn androidx.biometric.**
-dontwarn androidx.camera.**

# --- KryptUp ProGuard Rules ---

# Keep Web3j classes (blockchain interaction)
-keep class org.web3j.** { *; }
-dontwarn org.web3j.**

# Keep Retrofit / OkHttp / Gson
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep Firebase model classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep our data model classes (used in Firestore deserialization)
-keep class com.kryptup.kryptup.data.model.** { *; }
-keep class com.kryptup.kryptup.data.local.** { *; }
-keep class com.kryptup.kryptup.data.remote.** { *; }
-keepclassmembers class com.kryptup.kryptup.data.remote.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }
