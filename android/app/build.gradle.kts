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
        buildConfigField(
            "String",
            "UPDATE_ENDPOINT",
            "\"https://api.github.com/repos/benjaminchume-droid/Lumen-/releases/latest\""
        )
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"https://ryoewtikgwmyejrpjgnw.supabase.co\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ5b2V3dGlrZ3dteWVqcnBqZ253Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY3MDg3MzYsImV4cCI6MjEwMjI4NDczNn0.KNa80UeXpIuPlVEt5sFIq4TyYtIc2ykWe-k24m7O7Pg\""
        )
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
                storePassword = System.getenv("LUMEN_STORE_PASSWORD") ?: "lumenStorePass2026"
                keyAlias = System.getenv("LUMEN_KEY_ALIAS") ?: "lumen"
                keyPassword = System.getenv("LUMEN_KEY_PASSWORD") ?: "lumenStorePass2026"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
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
    implementation("androidx.compose.material:material-icons-extended")
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
