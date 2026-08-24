plugins {
  id("com.android.library")
  id("maven-publish")
}

android {
  namespace = "com.nextgen.ads"
  compileSdk = 36

  defaultConfig {
    minSdk = 24
    consumerProguardFiles("consumer-rules.pro")
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  publishing {
    singleVariant("release") {
      withSourcesJar()
    }
  }
}

dependencies {
  api("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.3.1")
  api("com.google.android.ump:user-messaging-platform:3.1.0")

  implementation("androidx.core:core-ktx:1.16.0")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.lifecycle:lifecycle-process:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
  implementation("com.facebook.shimmer:shimmer:0.5.0")
}

configurations.configureEach {
  exclude(group = "com.google.android.gms", module = "play-services-ads")
  exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

afterEvaluate {
  publishing {
    publications {
      create<MavenPublication>("release") {
        from(components["release"])
        groupId = (project.findProperty("PUBLISH_GROUP_ID") as? String) ?: "com.github.nguyenvanquyet0710"
        artifactId = (project.findProperty("PUBLISH_ARTIFACT_ID") as? String) ?: "nextgen-ads"
        version = (project.findProperty("PUBLISH_VERSION") as? String) ?: "1.1.0"

        pom {
          name.set("Google Mobile Ads Next-Gen Android Library")
          description.set("Android library wrapper and helpers for Google Mobile Ads Next-Gen SDK")
          url.set("https://github.com/nguyenvanquyet0710/nextgen-ads-android")
          licenses {
            license {
              name.set("The Apache License, Version 2.0")
              url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
          }
        }
      }
    }
    repositories {
      maven {
        name = "GitHubPackages"
        val repoOwner = (project.findProperty("gpr.owner") as? String)
          ?: System.getenv("GITHUB_REPOSITORY_OWNER")
          ?: "nguyenvanquyet0710"
        val repoName = (project.findProperty("gpr.repo") as? String)
          ?: "nextgen-ads-android"
        url = uri("https://maven.pkg.github.com/$repoOwner/$repoName")
        credentials {
          username = (project.findProperty("gpr.user") as? String) ?: System.getenv("GITHUB_ACTOR")
          password = (project.findProperty("gpr.key") as? String) ?: System.getenv("GITHUB_TOKEN")
        }
      }
    }
  }
}
