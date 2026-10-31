plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.recentr.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.recentr.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        // Android backs up the rules and the profile, and restores them on a new phone or a
        // reinstall.
        manifestPlaceholders["allowBackup"] = "true"
    }

    buildTypes {
        debug {
            // A debug build reinstalled starts afresh, from the onboarding, rather than with the
            // rules and profile of an earlier install restored from the backup.
            manifestPlaceholders["allowBackup"] = "false"
            // Settings › Developer › Language offers the pseudo-locales: accented English (en-XA)
            // shows untranslated text, Arabic (ar-XB) right-to-left layouts.
            isPseudoLocalesEnabled = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:home"))
    implementation(project(":feature:rules"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:focus"))
    implementation(project(":feature:insights"))

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
