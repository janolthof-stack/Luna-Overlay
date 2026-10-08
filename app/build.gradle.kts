plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "de.olti.lunaoverlay"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.olti.lunaoverlay"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("LUNA_VERSION_CODE")?.toIntOrNull() ?: 10
        versionName = "0.2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
