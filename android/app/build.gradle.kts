plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

android {
    namespace = "app.ninesevennine.twofactorauthenticator"
    compileSdk = 37

    buildToolsVersion = libs.versions.sdk.get()
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        applicationId = "app.ninesevennine.twofactorauthenticator"
        minSdk = 35
        targetSdk = 37

        // major
        // minor (000)
        // type (0 alpha, 1 beta, 2 release candidate, 3 release)
        // patch (000)
        versionCode = 10020001 // M mmm T ppp
        versionName = "1.2-alpha-1"

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

    buildTypes {
        release {
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
        val javaVersion = JavaVersion.toVersion(libs.versions.java.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles (for Google Play)
        includeInBundle = false
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
}