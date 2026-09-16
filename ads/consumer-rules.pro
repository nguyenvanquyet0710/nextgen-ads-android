# Next-Gen Ads Library – keep all classes (public, internal, anonymous callbacks)
-keep class com.nextgen.ads.** { *; }
-keepclassmembers class com.nextgen.ads.** { *; }

# Google Mobile Ads Next-Gen SDK & Mediation
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-keep interface com.google.android.libraries.ads.mobile.sdk.** { *; }
# AdMob mediation adapters (required for collapsible banners via AdMobAdapter)
-keep class com.google.android.gms.ads.mediation.** { *; }

# User Messaging Platform (UMP) SDK
-keep class com.google.android.ump.** { *; }

# Facebook Shimmer (used for Banner and Native ad shimmer placeholders)
-keep class com.facebook.shimmer.** { *; }

# Google Play Install Referrer (used for organic install detection)
-keep class com.android.installreferrer.** { *; }
-keep interface com.google.android.finsky.externalreferrer.** { *; }

# AndroidX WorkManager – prevent R8 from stripping WorkDatabase_Impl
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker

# AndroidX Room – keep generated *_Impl classes used by WorkManager
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Entity class * { *; }

# AndroidX Startup – prevent stripping of InitializationProvider
-keep class androidx.startup.** { *; }
