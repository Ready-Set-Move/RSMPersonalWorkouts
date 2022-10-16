plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    kotlin("android")
}

val composeVersion = "1.1.1"

kotlin {
    sourceSets {
        all {
            languageSettings.optIn("com.google.accompanist.permissions.ExperimentalPermissionsApi")
            languageSettings.optIn("androidx.compose.material.ExperimentalMaterialApi")
        }
    }
}

android {
    compileSdk = 33
    defaultConfig {
        applicationId = "com.readysetmove.personalworkouts.android"
        minSdk = 29
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        // Enables Jetpack Compose for this module
        compose = true
    }
    // Set both the Java and Kotlin compilers to target Java 8.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    dependencies {
        implementation(project(":shared"))
        implementation("androidx.compose.ui:ui:$composeVersion")
        // Integration with activities
        implementation("androidx.activity:activity-compose:1.4.0")
        // Material Design
        implementation("androidx.compose.material:material:$composeVersion")
        // Animations
        implementation("androidx.compose.animation:animation:$composeVersion")
        // Tooling support (Previews, etc.)
        implementation("androidx.compose.ui:ui-tooling:$composeVersion")
        // Icons
        implementation("androidx.compose.material:material-icons-extended:$composeVersion")
        // navigation
        implementation("androidx.navigation:navigation-compose:2.4.2")
        // Permission handling or jetpack compose
        implementation("com.google.accompanist:accompanist-permissions:0.24.7-alpha")
        // UI Tests
        androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
        debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
        // Koin DI
        implementation(libs.koin.core)
        implementation(libs.koin.android)
        implementation(libs.koin.androidx)
        // Napier logging
        implementation(libs.napier)
        // Firebase
        // - Import the Firebase BoM
        implementation(platform("com.google.firebase:firebase-bom:30.2.0"))
        // - When using the BoM, you don't specify versions in Firebase library dependencies
        // - TODO: enable analytics at release
        // implementation("com.google.firebase:firebase-analytics-ktx")
        // - Authentication
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("com.firebaseui:firebase-ui-auth:8.0.1")
        // -- transient dependencies (do we need the others? https://github.com/firebase/FirebaseUI-Android#auth)
        implementation("com.google.android.gms:play-services-auth:20.2.0")
        // - TODO: enable firestore
        // implementation("com.google.firebase:firebase-firestore-ktx")
    }
    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
    namespace = "com.readysetmove.personalworkouts.android"
}