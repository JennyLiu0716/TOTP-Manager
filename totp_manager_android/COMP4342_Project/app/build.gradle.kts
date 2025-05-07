plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "comp4342.totp_manager"
    compileSdk = 34

    defaultConfig {
        applicationId = "comp4342.totp_manager"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.zxing.core)  // Add ZXing core
    implementation(libs.zxing.android.embedded)  // Add ZXing android-embedded
    implementation(libs.gson)
    implementation(libs.biometric)
    implementation(libs.recyclerview)
    implementation(libs.room.runtime) // Reference to room-runtime
    implementation(libs.commons.codec)
    annotationProcessor(libs.room.compiler) // For Java projects
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}