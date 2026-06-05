plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {

    namespace = "com.example.taxisign"

    compileSdk = 36

    defaultConfig {

        applicationId = "com.example.taxisign"

        minSdk = 24

        targetSdk = 36

        versionCode = 1

        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }
}

dependencies {

    // UI

    implementation(
        "androidx.core:core-ktx:1.13.1"
    )

    implementation(
        "androidx.appcompat:appcompat:1.7.0"
    )

    implementation(
        "com.google.android.material:material:1.12.0"
    )

    implementation(
        "androidx.constraintlayout:constraintlayout:2.1.4"
    )

    implementation(
        "androidx.activity:activity-ktx:1.9.0"
    )

    // GOOGLE MAPS
    implementation("com.google.android.libraries.places:places:3.5.0")

    implementation(
        "com.google.android.gms:play-services-maps:18.2.0"
    )

    // GPS

    implementation(
        "com.google.android.gms:play-services-location:21.3.0"
    )

    // VOLLEY

    implementation(
        "com.android.volley:volley:1.2.1"
    )
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)

    // TEST

    testImplementation(
        "junit:junit:4.13.2"
    )

    androidTestImplementation(
        "androidx.test.ext:junit:1.2.1"
    )

    androidTestImplementation(
        "androidx.test.espresso:espresso-core:3.6.1"
    )
}