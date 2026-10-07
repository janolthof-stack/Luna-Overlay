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
        versionCode = 1
        versionName = "0.1"
    }
}
