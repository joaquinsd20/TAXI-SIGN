plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.taxisign.pro"
    compileSdk = 36 // Asegúrate de tener instalado el SDK 36 en tu Android Studio

    defaultConfig {
        applicationId = "com.taxisign.pro"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "1.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            storeFile = file("release/taxisign-key.jks") // Asegúrate que el archivo esté en esta ruta
            storePassword = "taxisign-key-2026"
            keyAlias = "key0"
            keyPassword = "aplicacionesmoviles2026-1"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // UI y Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.0")

    // --- NUEVAS DEPENDENCIAS AGREGADAS ---
    // Soluciona el error de CircleImageView
    implementation("de.hdodenhof:circleimageview:3.1.0")
    // Generación de QR
    implementation("com.google.zxing:core:3.5.4")
    // Escaneo de QR
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // -------------------------------------

    // Google Maps y GPS
    implementation("com.google.android.libraries.places:places:3.5.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Volley
    implementation("com.android.volley:volley:1.2.1")

    // Firebase (Gestionado por BoM)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // Librerías de Firebase
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}