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
        versionCode = System.getenv("LUNA_VERSION_CODE")?.toIntOrNull() ?: 11
        versionName = "0.3.0-prototype"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies { testImplementation("junit:junit:4.13.2") }
