# Proguard rules for Next-Gen Ads Library consumers
-keep public class com.nextgen.ads.** { *; }
-keepclassmembers class com.nextgen.ads.** { *; }

# Google Mobile Ads Next-Gen SDK rules
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-keep interface com.google.android.libraries.ads.mobile.sdk.** { *; }

# User Messaging Platform (UMP) SDK
-keep class com.google.android.ump.** { *; }
