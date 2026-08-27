# 🚀 NextGen Android Ads Library (v1.2.4)

[![Google Mobile Ads Next-Gen](https://img.shields.io/badge/Google_Mobile_Ads-Next--Gen_SDK_v1.3.1-4285F4?logo=google)](https://developers.google.com/admob/android/next-gen)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API_24+-3DDC84?logo=android)](https://developer.android.com)
[![GitHub Packages](https://img.shields.io/badge/GitHub_Packages-v1.2.4-181717?logo=github)](https://github.com/nguyenvanquyet0710/nextgen-ads-android/packages)

Thư viện Android Ads hiện đại, toàn diện và tối ưu hóa cao được xây dựng chuyên biệt trên nền tảng **Google Mobile Ads (GMA) Next-Gen SDK (`ads-mobile-sdk:1.3.1`)**. Tích hợp sẵn **User Messaging Platform (UMP / GDPR TCF v2.2)**, bộ đệm quảng cáo thông minh (**Preloader**), bảo vệ va chạm quảng cáo (**Full-screen ad collision prevention**), tự động điều phối âm thanh (**Global Audio Listener**), giao diện **Loading Dialog Material Design 3**, **Shimmer loading**, organic/test-device detection và combo **Inter → Native Fullscreen**.

---

## 📑 Mục lục

1. [Cài đặt thư viện (Installation)](#-1-cài-đặt-thư-viện-installation)
2. [Cấu hình & Khởi tạo SDK (NextGenAds)](#-2-cấu-hình--khởi-tạo-sdk-nextgenads)
3. [Tích hợp màn hình Splash & Xin quyền GDPR (Chuẩn nhất)](#-3-tích-hợp-màn-hình-splash--xin-quyền-gdpr-chuẩn-nhất)
4. [Tự động bật/tắt âm thanh game toàn cục (Audio Listener)](#-4-tự-động-bậttắt-âm-thanh-game-toàn-cục-audio-listener)
5. [App Open Ads (Quảng cáo mở ứng dụng)](#-5-app-open-ads-quảng-cáo-mở-ứng-dụng)
6. [Interstitial Ads (Quảng cáo xen kẽ)](#-6-interstitial-ads-quảng-cáo-xen-kẽ)
7. [Rewarded Ads (Quảng cáo video thưởng)](#-7-rewarded-ads-quảng-cáo-video-thưởng)
8. [Rewarded Interstitial Ads](#-8-rewarded-interstitial-ads)
9. [Banner Ads (Quảng cáo banner)](#-9-banner-ads-quảng-cáo-banner)
10. [Native Ads (Quảng cáo tự nhiên)](#-10-native-ads-quảng-cáo-tự-nhiên)
11. [Callbacks & Interfaces](#-11-callbacks--interfaces)
12. [Test Ad Unit IDs](#-12-test-ad-unit-ids)
13. [Native Layout — View IDs chuẩn](#-13-native-layout--view-ids-chuẩn)
14. [Hướng dẫn xuất bản (Publishing)](#-14-hướng-dẫn-xuất-bản-publishing)

---

## 📦 1. Cài đặt thư viện (Installation)

### Bước 1: Thêm GitHub Packages Repository

Trong tệp `settings.gradle.kts` của dự án bạn:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/nguyenvanquyet0710/nextgen-ads-android")
            credentials {
                username = project.findProperty("gpr.user") as? String
                    ?: System.getenv("GITHUB_ACTOR")
                    ?: "nguyenvanquyet0710"
                password = project.findProperty("gpr.key") as? String
                    ?: System.getenv("GITHUB_TOKEN")
                    ?: "YOUR_GITHUB_PERSONAL_ACCESS_TOKEN"
            }
        }
    }
}
```

### Bước 2: Khai báo Dependency

Trong `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.nguyenvanquyet0710:nextgen-ads:1.2.4")
}
```

> [!NOTE]
> **Điểm cải tiến vượt trội của Next-Gen SDK:** Bạn **KHÔNG CẦN** khai báo thẻ `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" ... />` trong `AndroidManifest.xml` như SDK cũ nữa!
>
> App ID được truyền trực tiếp linh hoạt qua mã nguồn Kotlin trong hàm `NextGenAds.initialize()` hoặc `NextGenAds.initAdmob()`.

---

## ⚡ 2. Cấu hình & Khởi tạo SDK (NextGenAds)

Object trung tâm: `NextGenAds` — khởi tạo SDK, cấu hình global, kiểm tra điều kiện show ad.

### AdConfig

```kotlin
NextGenAds.adConfig = AdConfig(
    isTestMode = true,          // true → tự động dùng Google sample test ad unit IDs
    isAdsEnabled = true,        // false → ẩn toàn bộ ads (premium user)
    isDebug = true,             // bật log debug
    isCheckTestDevice = false,  // detect test device qua headline native ad
    isCheckOrganic = false,     // true + organic install → không show ads
)
```

### Properties

| Property | Mô tả |
|---|---|
| `adConfig` | Cấu hình global ads |
| `canShowAds` | Có nên show ad không (premium / organic / test device) |
| `isInitialized` | SDK đã init chưa |
| `isFullScreenAdShowing` | Có full-screen ad đang hiện không |
| `onAdVisibilityChanged` | Callback khi full-screen ad mở/đóng |
| `referrerUrl` | Install referrer từ Google Play |
| `isOrganic` | User cài organic không (`null` nếu chưa fetch) |
| `isTestDevice` | Thiết bị test (detect qua headline) |

### Tất cả hàm NextGenAds

#### `initialize(context, appId, testDeviceIds, ageRestrictedTreatment, onComplete)`

Khởi tạo GMA Next-Gen SDK (API đầy đủ).

```kotlin
NextGenAds.initialize(
    context = applicationContext,
    appId = "ca-app-pub-3940256099942544~3347511713",
    testDeviceIds = listOf("YOUR_HASHED_TEST_DEVICE_ID"),
    onComplete = { status -> /* SDK ready */ },
)
```

#### `initAdmob(context, appId, isDebug, isEnableAds, isCheckTestDevice, isCheckOrganic, onComplete)`

Khởi tạo rút gọn + set `adConfig` + fetch install referrer (khuyên dùng).

```kotlin
NextGenAds.initAdmob(
    context = this,
    appId = "ca-app-pub-XXXX~YYYY",
    isDebug = BuildConfig.DEBUG,
    isEnableAds = true,
    isCheckTestDevice = true,
    isCheckOrganic = true,
) { /* SDK + referrer ready */ }
```

#### `canShowAds(context: Context): Boolean`

Kiểm tra `canShowAds` + có mạng không. Tất cả helper đều gọi hàm này trước khi load/show.

#### `resolveAdUnitId(realAdUnitId, format: AdFormat): String`

Tự thay bằng Google test ID nếu `isTestMode = true`.

#### `isTestAdUnitId(adUnitId): Boolean`

Kiểm tra có phải Google sample ID không.

#### `checkAdsTest(headline: String?)`

Detect test device qua headline native ad. Gọi sau khi native ad load:

```kotlin
NextGenAds.checkAdsTest(nativeAd.headline)
```

#### `setRequestConfiguration(testDeviceIds, ageRestrictedTreatment, maxAdContentRating)`

Cấu hình request global (test device, age rating…).

#### `isNetworkAvailable(context): Boolean`

Kiểm tra có mạng không.

#### `observeNetworkState(context): Flow<Boolean>`

Theo dõi trạng thái mạng realtime.

---

## 🌟 3. Tích hợp màn hình Splash & Xin quyền GDPR (Chuẩn nhất)

Luồng tối ưu: **Kiểm tra mạng** → **Xin quyền GDPR (UMP)** → **Khởi tạo SDK** → **Nạp & Hiện Splash AOA**:

```kotlin
class SplashActivity : AppCompatActivity() {

    private val isNavigating = AtomicBoolean(false)
    private val AOA_SPLASH_ID = "ca-app-pub-3940256099942544/9257395921"
    private val SPLASH_TIMEOUT_MS = 3500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if (!NextGenAds.isNetworkAvailable(this)) {
            navigateToMain()
            return
        }

        val consentManager = ConsentManager.getInstance(this)
        consentManager.gatherConsent(this) { error ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                if (consentManager.canRequestAds) {
                    NextGenAds.initAdmob(
                        context = this,
                        appId = "ca-app-pub-3940256099942544~3347511713",
                        isDebug = BuildConfig.DEBUG,
                    ) {
                        AppOpenAdManager.showSplashAoa(
                            activity = this,
                            adUnitId = AOA_SPLASH_ID,
                            timeoutMs = SPLASH_TIMEOUT_MS,
                            onComplete = { navigateToMain() },
                        )
                    }
                } else {
                    navigateToMain()
                }
            }
        }
    }

    private fun navigateToMain() {
        if (!isNavigating.compareAndSet(false, true)) return
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
```

### ConsentManager — Tất cả hàm

| Hàm | Mô tả |
|---|---|
| `getInstance(context): ConsentManager` | Singleton |
| `canRequestAds` (property) | User đã consent, có thể request ads |
| `isPrivacyOptionsRequired` (property) | Cần hiện privacy options form |
| `gatherConsent(activity, testDeviceId, isDebugGeographyEEA, timeoutMs, onConsentGatheringComplete)` | Xin consent GDPR, có timeout fallback |
| `showPrivacyOptionsForm(activity, onDismissListener)` | Hiện form cập nhật consent (Settings) |
| `resetConsent()` | Reset consent (chỉ dùng khi test) |

```kotlin
// Hiện privacy options trong Settings
ConsentManager.getInstance(this).showPrivacyOptionsForm(this) { error ->
    // User đã cập nhật consent
}
```

---

## 🎵 4. Tự động bật/tắt âm thanh game toàn cục (Audio Listener)

Để tránh hiện tượng nhạc game vẫn phát khi quảng cáo toàn màn hình bật lên, bạn chỉ cần đăng ký **1 dòng duy nhất** trong `MyApplication.onCreate()`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        NextGenAds.onAdVisibilityChanged = { isShowing ->
            if (isShowing) {
                SoundManager.pauseMusic()
            } else {
                SoundManager.resumeMusic()
            }
        }
    }
}
```

> Tự động hoạt động với: App Open, Interstitial, Rewarded, Rewarded Interstitial, Native Fullscreen.

---

## 📱 5. App Open Ads (Quảng cáo mở ứng dụng)

### Tự động hiển thị khi mở lại App (Resume)

Trong `MyApplication.kt`:

```kotlin
AppOpenAdManager.init(
    application = this,
    defaultAdUnitId = "ca-app-pub-3940256099942544/9257395921",
    autoShowOnResume = true,
    cooldownSeconds = 20,
)
AppOpenAdManager.disableForActivity(SplashActivity::class.java)
```

### AppOpenAdManager — Tất cả hàm

| Hàm | Mô tả |
|---|---|
| `init(application, defaultAdUnitId, autoShowOnResume, cooldownSeconds)` | Đăng ký lifecycle observer |
| `setAutoShowEnabled(enabled)` | Bật/tắt auto show on resume |
| `setCooldownSeconds(seconds)` | Giãn cách giữa các lần auto show |
| `setDefaultAdUnitId(adUnitId)` | Đổi ad unit ID mặc định |
| `disableForActivity(activityClass)` | Chặn AOA trên Activity (Splash, Payment…) |
| `enableForActivity(activityClass)` | Cho phép lại AOA trên Activity |
| `loadAd(context, adUnitId, callback)` | Load AOA vào cache |
| `isAdAvailable(): Boolean` | Ad sẵn sàng show (trong vòng 4 giờ) |
| `showAdIfAvailable(activity, onCompleteListener, callback)` | Show ad đã load, tự preload lại sau khi đóng |
| `showSplashAoa(activity, adUnitId, timeoutMs, onComplete)` | Load + show trên Splash, có timeout fallback |

```kotlin
// Show thủ công
AppOpenAdManager.loadAd(this, "ca-app-pub-XXXX/aoa-id", object : AdEventListener {
    override fun onAdLoaded() {
        AppOpenAdManager.showAdIfAvailable(
            activity = this@MainActivity,
            onCompleteListener = { /* done */ },
        )
    }
})
```

---

## 🎬 6. Interstitial Ads (Quảng cáo xen kẽ)

### Cách 1: Preloader + Loading Dialog (Khuyên dùng)

```kotlin
InterstitialAdHelper.startPreloader(adUnitId = "ca-app-pub-3940256099942544/1033173712")

InterstitialAdHelper.pollAndShowWithLoading(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/1033173712",
    loadingMessage = "Loading...",
    loadingDurationMs = 800L,
    onComplete = { goToNextLevel() },
)
```

### Cách 2: Single Load (Nạp đơn lẻ từng lượt)

```kotlin
InterstitialAdHelper.loadAndShowWithLoading(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/1033173712",
    loadingMessage = "Loading...",
    onComplete = { goToNextLevel() },
)
```

### Cách 3: Load + Show thủ công

```kotlin
InterstitialAdHelper.load("ca-app-pub-XXXX/inter-id") { ad, error ->
    if (ad != null) {
        InterstitialAdHelper.show(this, ad, object : AdEventListener {
            override fun onAdDismissed() { goToNextLevel() }
        })
    }
}
```

### Combo: Inter → Native Fullscreen

```kotlin
InterstitialAdHelper.showThenNativeFullScreen(
    activity = this,
    interAdUnitId = "ca-app-pub-XXXX/inter-id",
    nativeAdUnitId = "ca-app-pub-XXXX/native-id",
    nativeLayoutResId = R.layout.my_native_fullscreen,
    loadingMessage = "Loading...",
    onComplete = { goToNextLevel() },
)
```

### InterstitialAdHelper — Tất cả hàm

| Hàm | Mô tả |
|---|---|
| `startPreloader(adUnitId, preloadConfig, listener)` | Bắt đầu preload vào pool |
| `isPreloadedAdAvailable(adUnitId): Boolean` | Có ad preload sẵn không |
| `pollAndShow(activity, adUnitId, callback): Boolean` | Poll + show ngay |
| `pollAndShowWithLoading(activity, adUnitId, loadingMessage, loadingDurationMs, callback, onComplete)` | Poll + show kèm Loading Dialog |
| `load(adUnitId, callback)` | Load 1 interstitial |
| `show(activity, ad, callback)` | Show interstitial đã load |
| `loadAndShowWithLoading(activity, adUnitId, loadingMessage, callback, onComplete)` | Load + show kèm Loading Dialog |
| `showThenNativeFullScreen(activity, interAdUnitId, nativeAdUnitId, nativeLayoutResId, loadingMessage, onComplete)` | Inter → Native Fullscreen → action |

---

## 🎁 7. Rewarded Ads (Quảng cáo video thưởng)

```kotlin
RewardedAdHelper.startPreloader(adUnitId = "ca-app-pub-3940256099942544/5224354917")

RewardedAdHelper.pollAndShowWithLoading(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/5224354917",
    onUserEarnedReward = { rewardItem ->
        UserWallet.addCoins(rewardItem.amount)
    },
    onComplete = { /* done */ },
)
```

### RewardedAdHelper — Tất cả hàm

| Hàm | Mô tả |
|---|---|
| `startPreloader(adUnitId, preloadConfig, listener)` | Preload rewarded |
| `isPreloadedAdAvailable(adUnitId): Boolean` | Kiểm tra pool |
| `pollAndShow(activity, adUnitId, callback, onUserEarnedReward): Boolean` | Poll + show |
| `pollAndShowWithLoading(activity, adUnitId, loadingMessage, loadingDurationMs, callback, onUserEarnedReward, onComplete)` | Poll + show + Loading Dialog |
| `load(adUnitId, callback)` | Load 1 rewarded ad |
| `show(activity, ad, callback, onUserEarnedReward)` | Show rewarded đã load |
| `loadAndShowWithLoading(activity, adUnitId, loadingMessage, callback, onUserEarnedReward, onComplete)` | Load + show + Loading Dialog |

---

## 🏆 8. Rewarded Interstitial Ads

```kotlin
RewardedInterstitialHelper.startPreloader(adUnitId = "ca-app-pub-3940256099942544/5354046379")

RewardedInterstitialHelper.pollAndShowWithLoading(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/5354046379",
    onUserEarnedReward = { rewardItem ->
        UserWallet.addBonus(rewardItem.amount)
    },
    onComplete = { /* done */ },
)
```

### RewardedInterstitialHelper — Tất cả hàm

| Hàm | Mô tả |
|---|---|
| `startPreloader(adUnitId, preloadConfig, listener)` | Preload |
| `isPreloadedAdAvailable(adUnitId): Boolean` | Kiểm tra pool |
| `pollAndShow(activity, adUnitId, callback, onUserEarnedReward): Boolean` | Poll + show |
| `pollAndShowWithLoading(...)` | Poll + show + Loading Dialog |
| `destroyPreloader(adUnitId)` | Hủy preloader |
| `load(adUnitId, callback)` | Load 1 ad |
| `show(activity, ad, callback, onUserEarnedReward)` | Show ad đã load |
| `loadAndShowWithLoading(...)` | Load + show + Loading Dialog |

---

## 📐 9. Banner Ads (Quảng cáo banner)

### Cách 1: FrameLayout — Khuyên dùng (Shimmer + auto lifecycle)

XML:

```xml
<FrameLayout
    android:id="@+id/fl_banner"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Kotlin — **Mặc định 320×50** (banner classic sticky bottom):

```kotlin
BannerAdHelper.loadInto(
    container = binding.flBanner,
    adUnitId = "ca-app-pub-3940256099942544/9214589741",
    lifecycleOwner = viewLifecycleOwner,
)
```

Kotlin — **Adaptive full-width**:

```kotlin
BannerAdHelper.loadInto(
    container = binding.flBanner,
    adUnitId = "ca-app-pub-3940256099942544/9214589741",
    lifecycleOwner = viewLifecycleOwner,
    adSize = BannerAdHelper.getAdaptiveBannerAdSize(requireContext()),
)
```

### Cách 2: AdView có sẵn trong XML

```xml
<com.google.android.libraries.ads.mobile.sdk.banner.AdView
    android:id="@+id/adView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
BannerAdHelper.loadAdaptiveBanner(
    adView = binding.adView,
    adUnitId = "ca-app-pub-3940256099942544/9214589741",
    lifecycleOwner = this,
    callback = object : AdEventListener {
        override fun onAdLoaded() { /* ok */ }
        override fun onAdFailedToLoad(error: LoadAdError) { /* fail */ }
    },
)
```

### Collapsible Banner

```kotlin
BannerAdHelper.loadCollapsibleInto(
    container = binding.flBanner,
    adUnitId = "ca-app-pub-XXXX/banner-id",
    collapsiblePosition = "bottom", // hoặc "top"
    lifecycleOwner = viewLifecycleOwner,
)
```

### BannerAdHelper — Tất cả hàm

| Hàm | Mô tả |
|---|---|
| `getScreenWidthDp(context): Int` | Chiều rộng màn hình (dp) |
| `getAdaptiveBannerAdSize(context, widthDp?)` | Anchored adaptive (full width) |
| `getLargeAnchoredAdaptiveBannerAdSize(context, widthDp?)` | Large anchored adaptive |
| `getInlineAdaptiveBannerAdSize(context, widthDp?, maxHeightDp)` | Inline adaptive |
| `loadAdaptiveBanner(adView, adUnitId, lifecycleOwner, isLarge, callback)` | Load adaptive vào AdView |
| `loadBanner(adView, adUnitId, adSize, lifecycleOwner, callback)` | Load banner với AdSize tùy chọn |
| `bindLifecycle(adView, lifecycleOwner)` | Tự destroy AdView khi lifecycle destroy |
| `loadInto(container, adUnitId, lifecycleOwner, showShimmer, shimmerLayoutResId, adSize, isLarge, callback)` | Load + shimmer + crossfade vào FrameLayout. **Default: `AdSize.BANNER` (320×50)** |
| `loadCollapsibleInto(container, adUnitId, collapsiblePosition, lifecycleOwner, showShimmer, shimmerLayoutResId, callback)` | Collapsible banner |

---

## 🖼️ 10. Native Ads (Quảng cáo tự nhiên)

### Cách 1: Load + Show một lần (FrameLayout)

XML:

```xml
<FrameLayout
    android:id="@+id/fl_native"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
NativeAdHelper.loadInto(
    container = binding.flNative,
    adUnitId = "ca-app-pub-3940256099942544/2247696110",
    layoutResId = R.layout.native_ad_medium,
    lifecycleOwner = viewLifecycleOwner,
)
```

### Cách 2: Preload + Poll + Show

```kotlin
NativeAdHelper.startPreloader(adUnitId = "ca-app-pub-3940256099942544/2247696110")

NativeAdHelper.showPreloadedInto(
    container = binding.flNative,
    adUnitId = "ca-app-pub-3940256099942544/2247696110",
    layoutResId = R.layout.native_ad_medium,
    lifecycleOwner = viewLifecycleOwner,
)
```

### Cách 3: Load thủ công + bind View

```kotlin
NativeAdHelper.load("ca-app-pub-XXXX/native-id") { nativeAd, error ->
    if (nativeAd != null) {
        NativeAdHelper.bindEventCallback(nativeAd, object : AdEventListener {
            override fun onAdClicked() { /* ... */ }
            override fun onAdImpression() { /* ... */ }
        })
        NativeAdHelper.show(
            container = binding.flNative,
            nativeAd = nativeAd,
            layoutResId = R.layout.native_ad_medium,
            lifecycleOwner = viewLifecycleOwner,
        )
    }
}
```

### Cách 4: Native Fullscreen

```kotlin
// Load mới trong Activity
NativeAdHelper.showFullScreen(
    activity = this,
    adUnitId = "ca-app-pub-XXXX/native-id",
    layoutResId = com.nextgen.ads.R.layout.nextgen_native_fullscreen,
    onDismiss = { /* user đóng */ },
)

// Dùng ad đã preload
NativeAdHelper.startPreloader("ca-app-pub-XXXX/native-id")
NativeAdHelper.pollAndShowFullScreen(
    activity = this,
    adUnitId = "ca-app-pub-XXXX/native-id",
    onDismiss = { /* done */ },
)
```

> `pollAndShowFullScreen` chỉ show **Native fullscreen**, không show Interstitial. Combo Inter → Native dùng `InterstitialAdHelper.showThenNativeFullScreen`.

### Collapsible Native

```kotlin
NativeAdHelper.loadCollapsibleInto(
    container = binding.flNative,
    adUnitId = "ca-app-pub-XXXX/native-id",
    layoutResId = R.layout.native_ad_collapsible,
    collapsedHeightDp = 60,
    lifecycleOwner = viewLifecycleOwner,
)
```

Layout cần: `@+id/ivClose`, `@+id/ad_media`.

### NativeAdHelper — Tất cả hàm

| Hàm | Mô tả |
|---|---|
| `startPreloader(adUnitId, startMuted, preloadConfig, listener)` | Preload native ads |
| `isPreloadedAdAvailable(adUnitId): Boolean` | Có ad preload không |
| `pollAd(adUnitId): NativeAd?` | Lấy ad từ pool |
| `destroyPreloader(adUnitId)` | Hủy preloader |
| `load(adUnitId, startMuted, callback)` | Load 1 native ad |
| `bindEventCallback(nativeAd, callback)` | Gắn lifecycle callback |
| `populateNativeAdView(nativeAd, nativeAdView)` | Tự fill data + ẩn view null |
| `loadInto(container, adUnitId, layoutResId, lifecycleOwner, showShimmer, shimmerLayoutResId, startMuted, callback)` | Load + shimmer + inflate + populate |
| `show(container, nativeAd, layoutResId, lifecycleOwner, callback): Boolean` | Show ad đã load sẵn |
| `showPreloadedInto(container, adUnitId, layoutResId, lifecycleOwner, callback): Boolean` | Poll + show |
| `loadCollapsibleInto(container, adUnitId, layoutResId, collapsedHeightDp, lifecycleOwner, showShimmer, shimmerLayoutResId, callback)` | Native collapsible |
| `showFullScreen(activity, adUnitId, layoutResId, onDismiss)` | Native fullscreen Activity |
| `pollAndShowFullScreen(activity, adUnitId, layoutResId, onDismiss)` | Poll preloaded + fullscreen |

### AdShimmerView — Shimmer loading (dùng nội bộ, có thể gọi trực tiếp)

| Hàm | Mô tả |
|---|---|
| `showBannerShimmer(container): View` | Shimmer banner |
| `showNativeMediumShimmer(container): View` | Shimmer native medium |
| `showCustomShimmer(container, layoutResId): View` | Shimmer từ layout tùy chỉnh |
| `showNativeShimmerFromLayout(container, layoutResId): View` | Shimmer skeleton từ native template |
| `replaceShimmerWithAd(container, adView, fadeDurationMs)` | Crossfade shimmer → ad |
| `removeShimmerAndHide(container)` | Xóa shimmer + ẩn container |

---

## 📋 11. Callbacks & Interfaces

### AdEventListener

```kotlin
interface AdEventListener {
    fun onAdLoaded()
    fun onAdFailedToLoad(error: LoadAdError)
    fun onAdShowed()
    fun onAdDismissed()
    fun onAdFailedToShow(error: FullScreenContentError)
    fun onAdImpression()
    fun onAdClicked()
    fun onAdPaid(value: AdValue)
    fun onAdRefreshed()          // Banner refresh
    fun onAdFailedToRefresh(error: LoadAdError)
}
```

### AdPreloadListener

```kotlin
interface AdPreloadListener {
    fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo)
    fun onAdFailedToPreload(preloadId: String, error: LoadAdError)
    fun onAdsExhausted(preloadId: String)
}
```

Dùng với `startPreloader()` của Interstitial, Rewarded, Rewarded Interstitial, Native.

### OnShowAdCompleteListener

```kotlin
fun interface OnShowAdCompleteListener {
    fun onShowAdComplete()
}
```

Dùng với `AppOpenAdManager.showAdIfAvailable()`.

---

## 🧪 12. Test Ad Unit IDs

Tự dùng khi `NextGenAds.adConfig.isTestMode = true` (hoặc `initAdmob(isDebug = true)`):

| Format | ID |
|---|---|
| App ID | `ca-app-pub-3940256099942544~3347511713` |
| Banner | `ca-app-pub-3940256099942544/9214589741` |
| Interstitial | `ca-app-pub-3940256099942544/1033173712` |
| Rewarded | `ca-app-pub-3940256099942544/5224354917` |
| Rewarded Interstitial | `ca-app-pub-3940256099942544/5354046379` |
| Native | `ca-app-pub-3940256099942544/2247696110` |
| App Open | `ca-app-pub-3940256099942544/9257395921` |

Hoặc dùng constants: `TestAdUnitIds.BANNER`, `TestAdUnitIds.INTERSTITIAL`, …

---

## 🏷️ 13. Native Layout — View IDs chuẩn

Layout native (root = `NativeAdView`) dùng các ID sau để `autoBindViews` hoạt động:

| ID | View | Asset |
|---|---|---|
| `@+id/ad_headline` | TextView | Headline |
| `@+id/ad_body` | TextView | Body |
| `@+id/ad_call_to_action` | TextView/Button | CTA |
| `@+id/ad_app_icon` | ImageView | Icon |
| `@+id/ad_media` | MediaView | Media/Video |
| `@+id/ad_stars` | RatingBar | Star rating |
| `@+id/ad_advertiser` | TextView | Advertiser |
| `@+id/ad_store` | TextView | Store |
| `@+id/ad_price` | TextView | Price |

Collapsible native thêm: `@+id/ivClose`.

Built-in fullscreen layout: `com.nextgen.ads.R.layout.nextgen_native_fullscreen`

---

## 🛠️ 14. Hướng dẫn xuất bản (Publishing)

Xuất bản trực tiếp lên **GitHub Packages Maven Repository**:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :ads:publishReleasePublicationToGitHubPackagesRepository `
  "-PPUBLISH_GROUP_ID=com.github.nguyenvanquyet0710" `
  "-PPUBLISH_ARTIFACT_ID=nextgen-ads" `
  "-PPUBLISH_VERSION=1.2.4" `
  "-Pgpr.owner=nguyenvanquyet0710" `
  "-Pgpr.repo=nextgen-ads-android" `
  "-Pgpr.user=nguyenvanquyet0710" `
  "-Pgpr.key=YOUR_GITHUB_PAT_TOKEN"
```

Hoặc tạo GitHub Release tag `v1.2.4` → CI workflow tự publish.

---

## 📄 License

```text
Copyright 2026 NextGen Ads

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
