plugins {
    id("com.android.application")
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
    compileSdk = 32
    defaultConfig {
        applicationId = "com.readysetmove.personalworkouts.android"
        minSdk = 24
        targetSdk = 32
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
    }
    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
    namespace = "com.readysetmove.personalworkouts.android"
}