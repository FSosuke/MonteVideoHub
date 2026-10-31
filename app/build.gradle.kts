plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.fsosuke.montevideohub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fsosuke.montevideohub"
        // Xperia 5 V startet mit Android 13 – 26 hält die App trotzdem breit kompatibel.
        minSdk = 26
        targetSdk = 35
        // Jeder CI-Lauf = neue Build-Nummer; daran erkennt der In-App-Updater neue Versionen.
        val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = buildNumber
        versionName = "0.2.$buildNumber"

        buildConfigField("String", "UPDATE_REPO", "\"FSosuke/MonteVideoHub\"")
    }

    // Feste Signatur aus GitHub-Secrets (nie im Repo!), damit Updates über alte Versionen passen.
    // Lokal ohne diese Variablen wird mit dem normalen Debug-Schlüssel signiert.
    val keystorePath = System.getenv("SIGNING_KEYSTORE_FILE")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    val monteSigning = if (!keystorePath.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        signingConfigs.create("monte") {
            storeFile = file(keystorePath)
            storePassword = signingPassword
            keyAlias = "montevideohub"
            keyPassword = signingPassword
        }
    } else null

    buildTypes {
        debug {
            monteSigning?.let { signingConfig = it }
        }
        release {
            // NewPipeExtractor + Rhino brauchen eigene ProGuard-Regeln; vorerst ohne Minify.
            isMinifyEnabled = false
            signingConfig = monteSigning ?: signingConfigs.getByName("debug")
        }
    }

    testOptions {
        unitTests.all {
            it.environment("LIVE_TESTS", System.getenv("LIVE_TESTS") ?: "0")
            it.testLogging {
                showStandardStreams = true
                events("passed", "failed", "skipped")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    val media3 = "1.5.1"

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.0.4")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Wiedergabe: ExoPlayer + MediaSession (Hintergrund, Benachrichtigung, Kopfhörertasten)
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")
    implementation("androidx.media3:media3-ui:$media3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")

    // YouTube-Suche und Stream-Auflösung (wie NewPipe)
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
