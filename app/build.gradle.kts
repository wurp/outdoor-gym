import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// The app's ID and display name live in app.properties at the repo root.
val appProps = Properties().apply {
    // Read as UTF-8 so names like "Café Notes" survive (Properties.load(InputStream) assumes Latin-1).
    rootProject.file("app.properties").reader(Charsets.UTF_8).use { load(it) }
}

// CI passes -PversionCode=<run number> so every new build counts as an upgrade.
val buildVersionCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1

android {
    // Fixed code namespace, so source files never have to move.
    // The app's public identity is applicationId below.
    namespace = "com.vibedev.app"
    compileSdk = 36

    defaultConfig {
        applicationId = appProps.getProperty("applicationId")
        minSdk = 26
        targetSdk = 36
        versionCode = buildVersionCode
        versionName = "1.0.$buildVersionCode"
        resValue("string", "app_name", appProps.getProperty("appName"))
    }

    signingConfigs {
        // A committed debug key, so each test build installs over the last one.
        // Test builds only: the Vibe Store signs published apps with its own keys.
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Target Java 17 bytecode while building on whatever JDK is installed
// (cloud sessions and CI have JDK 21; a toolchain block would demand JDK 17).
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
