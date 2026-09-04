import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

android {
    namespace = "org.mromichov.guessthesong"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.mromichov.guessthesong"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["redirectSchemeName"] = "guessthesong"
        manifestPlaceholders["redirectHostName"] = "callback"

        val clientId = localProperties.getProperty("SPOTIFY_CLIENT_ID") ?: System.getenv("SPOTIFY_CLIENT_ID") ?: ""
        val clientSecret = localProperties.getProperty("SPOTIFY_CLIENT_SECRET") ?: System.getenv("SPOTIFY_CLIENT_SECRET") ?: ""

        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$clientId\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"$clientSecret\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.navigation:navigation-compose:2.9.8")

    // Dagger
    implementation("com.google.dagger:dagger:2.60.1")
    ksp("com.google.dagger:dagger-compiler:2.60.1")

    // Room
    implementation("androidx.room3:room3-runtime:3.0.2")
    ksp("androidx.room3:room3-compiler:3.0.2")

    implementation("ch.qos.logback:logback-classic:1.6.3")

    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")
    implementation("androidx.media3:media3-ui-compose-material3:1.11.0")

    // Ktor
    implementation("io.ktor:ktor-client-core:3.5.2")
    implementation("io.ktor:ktor-client-okhttp:3.5.2")
    implementation("io.ktor:ktor-client-logging:3.5.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.5.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.ktor:ktor-client-mock:3.5.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}