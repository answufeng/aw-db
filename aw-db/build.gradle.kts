plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.answufeng.db"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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

    lint {
        abortOnError = true
        warningsAsErrors = false
    }
}

ktlint {
    android.set(true)
    ignoreFailures = false
}

dependencies {
    api(libs.room.runtime)
    api(libs.room.ktx)
    api(libs.room.paging)
    implementation(libs.core.ktx)
    implementation(libs.annotation)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    api(libs.lifecycle.livedata.ktx)
    api(libs.kotlinx.serialization.json)
}

apply(from = "${rootDir}/gradle/publish.gradle.kts")
