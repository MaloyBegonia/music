plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "com.maloy.muzza"
        minSdk = 21
        targetSdk = 33
        versionCode = 20
        versionName = "2.0.1"
    }

    splits {
        abi {
            reset()
            isUniversalApk = true
        }
    }

    namespace = "com.maloy.muzza"

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "Debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appName"] = "Muzza"
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    kotlinOptions {
        freeCompilerArgs += "-Xcontext-receivers"
        jvmTarget = "10"
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(projects.composePersist)
    implementation(projects.composeRouting)
    implementation(projects.composeReordering)

    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ripple)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.coil)

    implementation(libs.palette)

    implementation(libs.exoplayer)

    implementation(libs.room)
    implementation("androidx.appcompat:appcompat:1.6.1")
    kapt(libs.room.compiler)

    implementation(projects.innertube)
    implementation(projects.kugou)

    coreLibraryDesugaring(libs.desugaring)

    // For loading and tinting drawables on older versions of the platform

    implementation("com.github.therealbush:translator:1.0.2")
}
