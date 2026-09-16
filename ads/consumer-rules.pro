# Proguard rules for Next-Gen Ads Library consumers
-keep public class com.nextgen.ads.** { *; }
-keepclassmembers class com.nextgen.ads.** { *; }

# Google Mobile Ads Next-Gen SDK rules
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-keep interface com.google.android.libraries.ads.mobile.sdk.** { *; }

# User Messaging Platform (UMP) SDK
-keep class com.google.android.ump.** { *; }

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
