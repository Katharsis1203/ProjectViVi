import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/*
 * Load private development values from local.properties.
 *
 * local.properties should not be committed to source control.
 */
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { inputStream ->
        localProperties.load(inputStream)
    }
}

val groqApiKey = localProperties
    .getProperty("GROQ_API_KEY", "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.example.visualvocab"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.visualvocab"

        minSdk = 24
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        /*
         * Makes the development API key available as:
         *
         * BuildConfig.GROQ_API_KEY
         *
         * This avoids placing the key directly in Kotlin source code.
         */
        buildConfigField(
            type = "String",
            name = "GROQ_API_KEY",
            value = "\"$groqApiKey\""
        )
    }

    buildTypes {
        debug {
            /*
             * The key comes from local.properties through BuildConfig.
             */
            isMinifyEnabled = false
        }

        release {
            /*
             * A production app should call Groq through a backend rather
             * than embedding a permanent private API key in the APK.
             */
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    /*
     * Jetpack Compose
     */
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    /*
     * Android and lifecycle
     */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1"
    )

    /*
     * Coroutines
     */
    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3"
    )

    /*
     * CameraX
     */
    val cameraXVersion = "1.6.0"

    implementation(
        "androidx.camera:camera-core:$cameraXVersion"
    )
    implementation(
        "androidx.camera:camera-camera2:$cameraXVersion"
    )
    implementation(
        "androidx.camera:camera-lifecycle:$cameraXVersion"
    )
    implementation(
        "androidx.camera:camera-view:$cameraXVersion"
    )

    /*
     * Machine learning and image recognition
     */
    implementation(
        "com.google.mlkit:image-labeling:17.0.9"
    )
    implementation(
        "com.google.mediapipe:tasks-vision:latest.release"
    )

    /*
     * Networking and JSON serialization for Groq
     */
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    /*
     * Retained temporarily in case another part of the application
     * still uses Gemini. Remove this dependency once we confirm that
     * no remaining source file imports the Gemini SDK.
     */
    implementation(
        "com.google.ai.client.generativeai:generativeai:0.9.0"
    )

    /*
     * Unit and instrumentation testing
     */
    testImplementation(libs.junit)

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )
    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )
    androidTestImplementation(
        libs.androidx.espresso.core
    )
    androidTestImplementation(
        libs.androidx.junit
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}