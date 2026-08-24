# 🚀 NextGen Android Ads Library (v1.0.7)

[![Google Mobile Ads Next-Gen](https://img.shields.io/badge/Google_Mobile_Ads-Next--Gen_SDK_v1.3.1-4285F4?logo=google)](https://developers.google.com/admob/android/next-gen)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API_23+-3DDC84?logo=android)](https://developer.android.com)
[![GitHub Packages](https://img.shields.io/badge/GitHub_Packages-v1.0.7-181717?logo=github)](https://github.com/nguyenvanquyet0710/nextgen-ads-android/packages)

Thư viện Android Ads hiện đại, toàn diện và tối ưu hóa cao được xây dựng chuyên biệt trên nền tảng **Google Mobile Ads (GMA) Next-Gen SDK (`ads-mobile-sdk:1.3.1`)**. Tích hợp sẵn **User Messaging Platform (UMP / GDPR TCF v2.2)**, bộ đệm quảng cáo thông minh (**Preloader**), bảo vệ va chạm quảng cáo (**Full-screen ad collision prevention**), tự động điều phối âm thanh (**Global Audio Listener**) và giao diện **Loading Dialog Material Design 3**.

---

## 📑 Mục lục
1. [Cài đặt thư viện (Installation)](#-1-cài-đặt-thư-viện-installation)
2. [Cấu hình AndroidManifest.xml](#-2-cấu-hình-androidmanifestxml)
3. [Tích hợp màn hình Splash & Xin quyền GDPR (Chuẩn nhất)](#-3-tích-hợp-màn-hình-splash--xin-quyền-gdpr-chuẩn-nhất)
4. [Tự động bật/tắt âm thanh game toàn cục (Audio Listener)](#-4-tự-động-bậttắt-âm-thanh-game-toàn-cục-audio-listener)
5. [App Open Ads (Quảng cáo mở ứng dụng)](#-5-app-open-ads-quảng-cáo-mở-ứng-dụng)
6. [Interstitial Ads (Quảng cáo xen kẽ)](#-6-interstitial-ads-quảng-cáo-xen-kẽ)
7. [Rewarded Ads (Quảng cáo video thưởng)](#-7-rewarded-ads-quảng-cáo-video-thưởng)
8. [Rewarded Interstitial Ads](#-8-rewarded-interstitial-ads)
9. [Adaptive Banner Ads](#-9-adaptive-banner-ads)
10. [Native Ads (Quảng cáo tự nhiên)](#-10-native-ads-quảng-cáo-tự-nhiên)
11. [Hướng dẫn xuất bản (Publishing)](#-11-hướng-dẫn-xuất-bản-publishing)

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
    implementation("com.github.nguyenvanquyet0710:nextgen-ads:1.0.7")
}
```

---

## ⚡ 2. Cấu hình App ID (Không cần AndroidManifest.xml)

> [!NOTE]
> **Điểm cải tiến vượt trội của Next-Gen SDK:** Bạn **KHÔNG CẦN** khai báo thẻ `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" ... />` trong `AndroidManifest.xml` như SDK cũ nữa!
> 
> App ID được truyền trực tiếp linh hoạt qua mã nguồn Kotlin trong hàm `NextGenAds.initialize(context, appId)`:
> ```kotlin
> NextGenAds.initialize(
>     context = this,
>     appId = "ca-app-pub-3940256099942544~3347511713" // App ID của bạn
> )
> ```


---

## 🌟 3. Tích hợp màn hình Splash & Xin quyền GDPR (Chuẩn nhất)

Luồng tối ưu: **Kiểm tra mạng** $\rightarrow$ **Xin quyền GDPR (UMP)** $\rightarrow$ **Khởi tạo SDK** $\rightarrow$ **Nạp & Hiện Splash AOA**:

```kotlin
class SplashActivity : AppCompatActivity() {

    private val isNavigating = AtomicBoolean(false)
    private val AOA_SPLASH_ID = "ca-app-pub-3940256099942544/9257395921"
    private val SPLASH_TIMEOUT_MS = 3500L // Thời gian chờ tối đa khi mạng yếu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 1. Kiểm tra kết nối mạng (Nếu offline -> Vào thẳng app)
        if (!NextGenAds.isNetworkAvailable(this)) {
            navigateToMain()
            return
        }

        // 2. Thu thập sự đồng ý GDPR / UMP Consent (Google Certified CMP)
        val consentManager = ConsentManager.getInstance(this)
        consentManager.gatherConsent(this) { error ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                if (consentManager.canRequestAds) {
                    // 3. Khởi tạo SDK Next-Gen
                    NextGenAds.initialize(this, "ca-app-pub-3940256099942544~3347511713") {
                        runOnUiThread {
                            if (isFinishing || isDestroyed) return@runOnUiThread
                            
                            // 4. Tải và hiện AOA (Timeout 3.5s an toàn)
                            AppOpenAdManager.showSplashAoa(
                                activity = this,
                                adUnitId = AOA_SPLASH_ID,
                                timeoutMs = SPLASH_TIMEOUT_MS,
                                onComplete = {
                                    // CHỈ gọi khi user bấm nút tắt (X) hoặc hết thời gian timeout
                                    navigateToMain()
                                }
                            )
                        }
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

---

## 🎵 4. Tự động bật/tắt âm thanh game toàn cục (Audio Listener)

Để tránh hiện tượng nhạc game vẫn phát khi quảng cáo toàn màn hình bật lên, bạn chỉ cần đăng ký **1 dòng duy nhất** trong `MyApplication.onCreate()`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Tự động lắng nghe BẤT KỲ quảng cáo nào (AOA, Inter, Reward) bật lên hoặc tắt đi
        NextGenAds.onAdVisibilityChanged = { isShowing ->
            if (isShowing) {
                // Có quảng cáo đang hiển thị -> Dừng toàn bộ nhạc nền
                SoundManager.pauseMusic()
            } else {
                // Quảng cáo đã đóng lại -> Tiếp tục phát nhạc
                SoundManager.resumeMusic()
            }
        }
    }
}
```

---

## 📱 5. App Open Ads (Quảng cáo mở ứng dụng)

### Tự động hiển thị khi mở lại App (Resume):

Trong `MyApplication.kt`:
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Đăng ký tự động show khi resume từ background
        AppOpenAdManager.init(
            application = this,
            defaultAdUnitId = "ca-app-pub-3940256099942544/9257395921",
            autoShowOnResume = true,
            cooldownSeconds = 20 // Giãn cách 20s giữa các lần hiện
        )
        
        // Vô hiệu hóa auto show trên các màn hình nhạy cảm (Splash, Payment...)
        AppOpenAdManager.disableForActivity(SplashActivity::class.java)
    }
}
```

---

## 🎬 6. Interstitial Ads (Quảng cáo xen kẽ)

### Cách 1: Sử dụng Preloader + Loading Dialog (Khuyên dùng - Chuẩn Google Next-Gen)

```kotlin
// 1. Nạp trước (Preload) từ sớm (Splash hoặc Home)
InterstitialAdHelper.startPreloader(adUnitId = "ca-app-pub-3940256099942544/1033173712")

// 2. Hiển thị kèm Dialog Loading Material Design 3 (Mặc định 800ms)
InterstitialAdHelper.pollAndShowWithLoading(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/1033173712",
    loadingMessage = "Loading...", // Tuỳ chỉnh text loading
    loadingDurationMs = 800L,
    onComplete = {
        // Chuyển màn hình / tiếp tục chơi sau khi đóng ad (hoặc khi không có ad)
        goToNextLevel()
    }
)
```

### Cách 2: Single Load (Nạp đơn lẻ từng lượt)

```kotlin
InterstitialAdHelper.loadAndShowWithLoading(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/1033173712",
    loadingMessage = "Loading...",
    onComplete = {
        goToNextLevel()
    }
)
```

---

## 🎁 7. Rewarded Ads (Quảng cáo video thưởng)

```kotlin
// 1. Nạp trước
RewardedAdHelper.startPreloader(adUnitId = "ca-app-pub-3940256099942544/5224354917")

// 2. Hiển thị với Loading Dialog & Nhận thưởng
RewardedAdHelper.pollAndShowWithLoading(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/5224354917",
    onUserEarnedReward = { rewardItem ->
        // Cộng coin / nhận quà
        UserWallet.addCoins(rewardItem.amount)
    },
    onComplete = {
        // Hoàn tất quảng cáo
    }
)
```

---

## 🏆 8. Rewarded Interstitial Ads

```kotlin
RewardedInterstitialHelper.startPreloader(adUnitId = "ca-app-pub-3940256099942544/5354046379")

RewardedInterstitialHelper.pollAndShowWithLoading(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/5354046379",
    onUserEarnedReward = { rewardItem ->
        UserWallet.addBonus(rewardItem.amount)
    }
)
```

---

## 📐 9. Adaptive Banner Ads

### XML Layout:
```xml
<com.google.android.libraries.ads.mobile.sdk.banner.AdView
    android:id="@+id/adView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

### Kotlin:
```kotlin
// Tự động giải phóng AdView khi Activity onDestroy (Tránh Memory Leak)
BannerAdHelper.loadAdaptiveBanner(
    adView = binding.adView,
    adUnitId = "ca-app-pub-3940256099942544/9214589741",
    lifecycleOwner = this, // Tự động bind vòng đời
    callback = object : AdEventListener {
        override fun onAdLoaded() {
            // Banner nạp thành công
        }
        override fun onAdFailedToLoad(error: LoadAdError) {
            // Xử lý khi lỗi nạp
        }
    }
)
```

---

## 🖼️ 10. Native Ads (Quảng cáo tự nhiên)

```kotlin
// 1. Nạp trước với video tắt tiếng mặc định (startMuted = true)
NativeAdHelper.startPreloader(adUnitId = "ca-app-pub-3940256099942544/2247696110")

// 2. Lấy ad từ kho đệm và bind vào View
val nativeAd = NativeAdHelper.pollAd("ca-app-pub-3940256099942544/2247696110")
if (nativeAd != null) {
    NativeAdHelper.bindEventCallback(nativeAd, object : AdEventListener {
        override fun onAdClicked() { /* ... */ }
        override fun onAdImpression() { /* ... */ }
    })
    // Gán dữ liệu vào NativeAdView của bạn
}
```

---

## 🛠️ 11. Hướng dẫn xuất bản (Publishing)

Xuất bản trực tiếp lên **GitHub Packages Maven Repository**:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :ads:publishReleasePublicationToGitHubPackagesRepository `
  "-PPUBLISH_GROUP_ID=com.github.nguyenvanquyet0710" `
  "-PPUBLISH_ARTIFACT_ID=nextgen-ads" `
  "-PPUBLISH_VERSION=1.0.7" `
  "-Pgpr.owner=nguyenvanquyet0710" `
  "-Pgpr.repo=nextgen-ads-android" `
  "-Pgpr.user=nguyenvanquyet0710" `
  "-Pgpr.key=YOUR_GITHUB_PAT_TOKEN"
```

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
