# NextGen Android Ads Library

Thư viện Android Ads hiện đại được xây dựng dựa trên **Google Mobile Ads (GMA) Next-Gen SDK (`ads-mobile-sdk`)**, tích hợp sẵn **User Messaging Platform (UMP / GDPR)**, quản lý vòng đời ứng dụng tự động, hỗ trợ **Preloader**, **Single-load**, và hỗ trợ xuất bản lên **GitHub Packages** / **JitPack** / **Maven Local**.

---

## 📦 1. Cài đặt thư viện (Installation)

### Cách 1: Tích hợp qua GitHub Packages

Trong tệp `settings.gradle.kts` của dự án bạn:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/OWNER/REPO")
            credentials {
                username = project.findProperty("gpr.user") as? String ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as? String ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Trong `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.OWNER:nextgen-ads:1.0.0")
}
```

---

### Cách 2: Tích hợp qua JitPack

Trong `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Trong `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.USERNAME:REPO_NAME:1.0.0")
}
```

---

### Cách 3: Sử dụng trực tiếp Submodule nội bộ

Nếu bạn đưa module `:ads` vào cùng source code dự án Android của bạn:
- Thêm `include(":ads")` vào `settings.gradle.kts`.
- Thêm `implementation(project(":ads"))` vào `app/build.gradle.kts`.

---

## ⚙️ 2. Cấu hình `AndroidManifest.xml`

Thêm App ID của AdMob vào thẻ `<application>` trong `AndroidManifest.xml`:

```xml
<manifest ...>
    <application ...>
        <!-- Sample AdMob App ID hoặc ID ứng dụng thật của bạn -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-3940256099942544~3347511713"/>
    </application>
</manifest>
```

---

## 🚀 3. Hướng dẫn sử dụng (Quick Start Guide)

### 3.1. Khởi tạo SDK & Xin quyền GDPR / UMP Consent

Trong `SplashActivity` hoặc `Application`:

```kotlin
import com.nextgen.ads.NextGenAds
import com.nextgen.ads.consent.ConsentManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Quản lý UMP Consent (GDPR)
        val consentManager = ConsentManager.getInstance(this)
        consentManager.gatherConsent(this, testDeviceId = "YOUR_TEST_DEVICE_HASH_ID") { error ->
            if (consentManager.canRequestAds) {
                // 2. Khởi tạo NextGen Ads SDK
                NextGenAds.initialize(
                    context = this,
                    appId = "ca-app-pub-3940256099942544~3347511713",
                    testDeviceIds = listOf("YOUR_TEST_DEVICE_HASH_ID")
                ) { status ->
                    // Khởi tạo thành công
                }
            }
        }
    }
}
```

---

### 3.2. App Open Ads (Quảng cáo mở app)

Tự động lắng nghe vòng đời app và hiển thị khi app chuyển từ background lên foreground:

```kotlin
import com.nextgen.ads.appopen.AppOpenAdManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Khởi tạo và đăng ký tự động show khi resume
        AppOpenAdManager.init(
            application = this,
            defaultAdUnitId = "ca-app-pub-3940256099942544/9257395921",
            autoShowOnResume = true
        )
        // Nạp trước quảng cáo mở app
        AppOpenAdManager.loadAd(this)
    }
}
```

Để hiển thị thủ công (ví dụ tại màn hình Splash):

```kotlin
AppOpenAdManager.showAdIfAvailable(activity) {
    // Callback khi ad hiển thị xong hoặc đóng
    startMainActivity()
}
```

---

### 3.3. Banner Ads (Adaptive Banner)

Trong XML Layout:
```xml
<com.google.android.libraries.ads.mobile.sdk.banner.AdView
    android:id="@+id/adView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Trong Kotlin:
```kotlin
import com.nextgen.ads.banner.BannerAdHelper
import com.nextgen.ads.callbacks.AdEventListener

BannerAdHelper.loadAdaptiveBanner(
    adView = binding.adView,
    adUnitId = "ca-app-pub-3940256099942544/9214589741",
    lifecycleOwner = viewLifecycleOwner,
    callback = object : AdEventListener {
        override fun onAdLoaded() {
            // Banner nạp thành công
        }
    }
)
```

---

### 3.4. Interstitial Ads (Quảng cáo xen kẽ)

#### Cách 1: Sử dụng Preloader (Khuyên dùng trong Next-Gen SDK)

```kotlin
import com.nextgen.ads.interstitial.InterstitialAdHelper

// 1. Bắt đầu preloader (gọi khi vào màn hình hoặc khởi động app)
InterstitialAdHelper.startPreloader(
    adUnitId = "ca-app-pub-3940256099942544/1033173712"
)

// 2. Hiển thị khi sẵn sàng
if (InterstitialAdHelper.isPreloadedAdAvailable(adUnitId)) {
    InterstitialAdHelper.pollAndShow(activity, adUnitId, object : AdEventListener {
        override fun onAdDismissed() {
            // Chuyển màn hình / tiếp tục game
        }
    })
}
```

#### Cách 2: Single Load (Nạp đơn lẻ theo lượt)

```kotlin
InterstitialAdHelper.load("ca-app-pub-3940256099942544/1033173712") { ad, error ->
    ad?.let {
        InterstitialAdHelper.show(activity, it, object : AdEventListener {
            override fun onAdDismissed() {
                // Xử lý sau khi xem xong
            }
        })
    }
}
```

---

### 3.5. Rewarded Ads (Quảng cáo nhận thưởng)

#### Preloader:
```kotlin
import com.nextgen.ads.rewarded.RewardedAdHelper

// Bắt đầu preloader
RewardedAdHelper.startPreloader(adUnitId = "ca-app-pub-3940256099942544/5224354917")

// Hiển thị quảng cáo
RewardedAdHelper.pollAndShow(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/5224354917",
    onUserEarnedReward = { rewardItem ->
        // Cộng coin / quà cho user
        addCoins(rewardItem.amount)
    }
)
```

---

### 3.6. Rewarded Interstitial Ads

```kotlin
import com.nextgen.ads.rewardedinterstitial.RewardedInterstitialHelper

RewardedInterstitialHelper.startPreloader(adUnitId = "ca-app-pub-3940256099942544/5354046379")

RewardedInterstitialHelper.pollAndShow(
    activity = this,
    adUnitId = "ca-app-pub-3940256099942544/5354046379",
    onUserEarnedReward = { reward ->
        // Trao thưởng
    }
)
```

---

### 3.7. Native Ads (Quảng cáo tự nhiên)

```kotlin
import com.nextgen.ads.nativead.NativeAdHelper

// 1. Single Load
NativeAdHelper.load(adUnitId = "ca-app-pub-3940256099942544/2247696110") { nativeAd, error ->
    nativeAd?.let { ad ->
        // Gắn dữ liệu nativeAd vào view UI của bạn
    }
}

// 2. Preloader
NativeAdHelper.startPreloader(adUnitId = "ca-app-pub-3940256099942544/2247696110")
val ad = NativeAdHelper.pollAd(adUnitId)
```

---

## 🛠️ 4. Hướng dẫn Publish lên GitHub Packages

### Tự động với GitHub Actions (CI/CD):
1. Đẩy code lên repository GitHub.
2. Tạo một **Release** mới (hoặc tag `v1.0.0`) trên GitHub.
3. Workflow `.github/workflows/publish.yml` sẽ tự động kích hoạt, build AAR và xuất bản lên GitHub Packages của repository!

### Xuất bản thủ công từ máy tính cá nhân:
Chạy lệnh Gradle sau:

```bash
./gradlew :ads:publishReleasePublicationToGitHubPackagesRepository \
  -PPUBLISH_GROUP_ID="com.github.YOUR_USERNAME" \
  -PPUBLISH_ARTIFACT_ID="nextgen-ads" \
  -PPUBLISH_VERSION="1.0.0" \
  -Pgpr.owner="YOUR_USERNAME" \
  -Pgpr.repo="YOUR_REPO_NAME" \
  -Pgpr.user="YOUR_GITHUB_USERNAME" \
  -Pgpr.key="YOUR_GITHUB_PERSONAL_ACCESS_TOKEN"
```

Hoặc xuất bản ra kho lưu trữ cục bộ trên máy để test:
```bash
./gradlew :ads:publishToMavenLocal
```
