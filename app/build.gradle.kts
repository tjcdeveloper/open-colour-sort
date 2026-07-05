plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "uk.co.tjcdeveloper.opencoloursort"
    compileSdk = 36

    defaultConfig {
        applicationId = "uk.co.tjcdeveloper.opencoloursort"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // Testing helper: unlocks every level. false here and in release so it
        // can only ever be true in local debug builds.
        buildConfigField("boolean", "UNLOCK_ALL_LEVELS", "false")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "UNLOCK_ALL_LEVELS", "true")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "UNLOCK_ALL_LEVELS", "false")
        }
    }
    compileOptions {
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
    testOptions {
        unitTests.all {
            // Forward the BakeLevelsTool codegen flag to the test JVM.
            it.systemProperty("bakeLevels", providers.gradleProperty("bakeLevels").getOrElse("false"))
            // The analysis tools hold large search frontiers in memory.
            it.maxHeapSize = "4g"
            it.testLogging { showStandardStreams = true }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
