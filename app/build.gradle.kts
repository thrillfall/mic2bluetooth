plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.thrillfall.mic2bluetooth"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.thrillfall.mic2bluetooth"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            enableV1Signing = true
            enableV2Signing = true
            storeFile = file(providers.gradleProperty("releaseStoreFile").getOrElse("keystore"))
            storePassword = providers.gradleProperty("releaseStorePassword").getOrElse("password")
            keyAlias = providers.gradleProperty("releaseKeyAlias").getOrElse("alias")
            keyPassword = providers.gradleProperty("releaseKeyPassword").getOrElse("password")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (providers.gradleProperty("releaseStoreFile").isPresent) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
