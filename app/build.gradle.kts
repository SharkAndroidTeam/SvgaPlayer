import com.shark.svgaplayer.setupAppModule

plugins {
    id("com.android.application")
    id("kotlin-android")
}

setupAppModule {
    defaultConfig {
        applicationId = "com.shark.svgaplayer"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("shrinker-rules.pro", "shrinker-rules-android.pro")
            signingConfig = signingConfigs["debug"]
        }
    }
    buildFeatures {
        viewBinding = true
    }
}
dependencies {
    api(projects.svgaplayerBase)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.navigation.fragment.ktx)
    implementation("androidx.navigation:navigation-ui-ktx:2.4.2")
}
