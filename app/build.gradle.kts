import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.oasisfeng.todo.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oasisfeng.todo.wear"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) localProperties.load(localPropertiesFile.inputStream())
        val todoistClientId = localProperties.getProperty("TODOIST_CLIENT_ID")
            ?: System.getenv("TODOIST_CLIENT_ID")
            ?: throw GradleException("TODOIST_CLIENT_ID not found in local.properties or environment variables.")
        val todoistClientSecret = localProperties.getProperty("TODOIST_CLIENT_SECRET")
            ?: System.getenv("TODOIST_CLIENT_SECRET")
            ?: throw GradleException("TODOIST_CLIENT_SECRET not found in local.properties or environment variables.")

        buildConfigField("String", "TODOIST_CLIENT_ID", "\"$todoistClientId\"")
        buildConfigField("String", "TODOIST_CLIENT_SECRET", "\"$todoistClientSecret\"")
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
    buildFeatures.buildConfig = true
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)

    // Horologist provides helpful wrappers for Tiles development
    implementation(libs.horologist.tiles)
    // Core Tile dependencies for creating the service and layouts
    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout.material)
    implementation(libs.wear.protolayout.material3)
    // Tooling dependencies for previewing tiles in Android Studio.
    debugImplementation(libs.wear.tiles.renderer)
    debugImplementation(libs.wear.tooling.preview)
    // The tile preview code is in the same file as the tiles themselves, so this dependency must be available to release builds, not just debug builds.
    implementation(libs.wear.tiles.tooling)
    implementation(libs.wear.tiles.tooling.preview)

    implementation(libs.watchface.complications.data.source.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.wear.phone.interactions)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
