plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.annotatex_mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.annotatex_mobile"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:2.1.2")
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation ("com.google.firebase:firebase-analytics-ktx")
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.pspdfkit:pspdfkit:2024.7.0")
    implementation ("com.google.firebase:firebase-firestore-ktx")
    implementation (libs.recyclerview)
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    implementation ("com.airbnb.android:lottie:6.6.0")
    implementation ("androidx.core:core-ktx:1.13.1")



}