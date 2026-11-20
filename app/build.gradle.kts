plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeystorePath = System.getenv("CRYSTAL_KEYSTORE_PATH")
val releaseKeystorePassword = System.getenv("CRYSTAL_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("CRYSTAL_KEY_ALIAS")
val releaseKeyPassword = System.getenv("CRYSTAL_KEY_PASSWORD")
val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank() &&
    !releaseKeystorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.vea.cementbagcounter"
    compileSdk = 35

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    defaultConfig {
        applicationId = "com.vea.cementbagcounter"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
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

    androidResources {
        noCompress += "onnx"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // AI - ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")
}
