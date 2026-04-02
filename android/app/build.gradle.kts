plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.ninesevennine.twofactorauthenticator"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.ninesevennine.twofactorauthenticator"
        minSdk = 34
        targetSdk = 36

        // major
        // minor (000)
        // type (0 alpha, 1 beta, 2 release candidate, 3 release)
        // patch (000)
        versionCode = 10002004 // M mmm T ppp
        versionName = "1.0.0-rc-4"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }

    flavorDimensions += "store"

    productFlavors {
        create("standard") {
            dimension = "store"
            applicationId = "app.ninesevennine.twofactorauthenticator"
        }
        create("accrescent") {
            dimension = "store"
            applicationId = "app.ninesevennine.twofactorauthenticator.accrescent"
        }
        create("play") {
            dimension = "store"
            applicationId = "app.ninesevennine.twofactorauthenticator.play"
        }
    }

    signingConfigs {
        create("release") {
            enableV1Signing = false
            enableV2Signing = true // Required for Obtainium
            enableV3Signing = false // Temporarily disable V3 signing
            enableV4Signing = false // Temporarily disable V4 signing
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets["main"].jniLibs.directories.add("../../appcore/target/jniLibs")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.biometric)
    implementation(libs.reorderable)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.zxing)
}