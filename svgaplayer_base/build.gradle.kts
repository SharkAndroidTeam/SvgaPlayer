plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 21
        targetSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro", "consumer-rules.pro")
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    api(libs.androidx.annotation)
    api(libs.androidx.appcompat.resources)
    api(libs.androidx.collection)
    api(libs.androidx.collection.ktx)
    api(libs.androidx.core)
    api(libs.androidx.exifinterface)
    api(libs.androidx.lifecycle.runtime)
    api(libs.coroutines.android)
    api(libs.okhttp)
    api(libs.okio)
    api(libs.wire)
}
