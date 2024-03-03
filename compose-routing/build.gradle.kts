plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.maloy.compose.routing"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
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

dependencies {
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
}
