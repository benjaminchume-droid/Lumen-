plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.lumen.reader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lumen.reader"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GITHUB_OWNER", "\"benjaminchume-droid\"")
        buildConfigField("String", "GITHUB_REPO", "\"Lumen-\"")
        buildConfigField("String", "UPDATE_ENDPOINT", "\"https://api.github.com/repos/benjaminchume-droid/Lumen-/releases/latest\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("LUMEN_KEYSTORE_PATH") ?: "release.keystore"
            val candidate = file(storeFilePath)
            val alt = file("release.keystore")
            val resolved = when {
                candidate.exists() -> candidate
                alt.exists() -> alt
                else -> null
            }
            if (resolved != null) {
                storeFile = resolved
                storePassword = System.getenv("LUMEN_STORE_PASSWORD") ?: ""
                keyAlias = System.getenv("LUMEN_KEY_ALIAS") ?: "lumen"
                keyPassword = System.getenv("LUMEN_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseCfg = signingConfigs.getByName("release")
            val hasKeystore = releaseCfg.storeFile?.exists() == true &&
                !releaseCfg.storePassword.isNullOrBlank() &&
                !releaseCfg.keyPassword.isNullOrBlank()
            if (hasKeystore) {
                signingConfig = releaseCfg
            } else {
                // CI without secrets should fail earlier; local debug release stays unsigned
                println("WARNING: release signing config incomplete — building unsigned release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
