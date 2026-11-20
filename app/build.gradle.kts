plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wlqboard.nativeapp"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        noCompress += listOf("onnx", "ort", "bin", "mdl", "txt", "json")
    }

    defaultConfig {
        applicationId = "com.wlqboard.nativeapp"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "1.3.0"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }
}

dependencies {
    implementation("com.alphacephei:vosk-android:0.3.75")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
}
