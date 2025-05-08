import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

val apikeyPropertiesFile: File = rootProject.file("apikey.properties")
val apikeyProperties: Properties = Properties()
apikeyProperties.load(FileInputStream(apikeyPropertiesFile))

android {
    namespace = "com.example.techpaperjournal"
    compileSdk = 35
    buildFeatures.buildConfig = true

    defaultConfig {
        applicationId = "com.example.techpaperjournal"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_KEY", apikeyProperties.getProperty("OPENAI_API_KEY"))
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        //buildConfig = true
    }
}

dependencies {
    // Core Android Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle & ViewModel (KTX Extensions)
    implementation(libs.androidx.lifecycle.livedata.ktx) // LiveData support
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // ViewModel support
    implementation(libs.androidx.lifecycle.runtime.ktx) // Lifecycle-aware coroutines

    // Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx) // Navigation support for Fragments
    implementation(libs.androidx.navigation.ui.ktx) // UI Navigation helpers (e.g. BottomNav)

    // Firebase (Bill of Materials manages versions for you)
    implementation(platform(libs.firebase.bom)) // BoM for Firebase
    implementation(libs.firebase.analytics) // Firebase Analytics
    implementation(libs.firebase.firestore) // Firestore database
    implementation(libs.firebase.storage.ktx) // Firebase Storage (KTX)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core) // Core coroutines
    implementation(libs.kotlinx.coroutines.android) // Android-specific coroutines

    // Network (Retrofit + OkHttp + Gson)
    implementation(libs.retrofit) // Retrofit HTTP client
    implementation(libs.converter.gson) // Retrofit converter for JSON
    implementation(libs.okhttp) // OkHttp networking
    implementation(libs.kotlinx.serialization.json) // Kotlinx serialization

    // UI Utilities
    implementation(libs.google.flexbox) // Flexbox layout
    implementation(libs.tom.roush.pdfbox.android) // PDFBox for PDF metadata parsing
    implementation(libs.richeditor.android) // Rich text editor
    implementation(libs.github.colorpicker) // Color picker

    // Testing
    testImplementation(libs.junit) // Unit testing
    androidTestImplementation(libs.androidx.junit) // Android JUnit support
    androidTestImplementation(libs.androidx.espresso.core) // UI testing with Espresso
}
