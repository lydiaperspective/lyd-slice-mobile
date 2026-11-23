plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.lydiaperspective.lydslice"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lydiaperspective.lydslice"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.2.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    packaging { jniLibs.useLegacyPackaging = false }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.webkit:webkit:1.15.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.navigation:navigation-compose:2.9.5")

    debugImplementation("androidx.compose.ui:ui-tooling")

    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

    val fetchOrcaEngine by tasks.registering(Exec::class) {
        group = "build setup"
        description = "Fetches the checksum-verified OrcaSlicer ARM64 native engine"
        commandLine("bash", rootProject.file("scripts/fetch-orca-mobile-engine.sh").absolutePath)
        inputs.property("orcaEngineArchiveSha256", "fdcf1b82e91a3897e6ec860cedc6e0cb79ed49cfd037a9f5274126edd2560388")
        outputs.files(
            layout.projectDirectory.file("src/main/jniLibs/arm64-v8a/libslic3r.so"),
            layout.projectDirectory.file("src/main/jniLibs/arm64-v8a/libgmp.so"),
            layout.projectDirectory.file("src/main/jniLibs/arm64-v8a/libgmpxx.so"),
            layout.projectDirectory.file("src/main/jniLibs/arm64-v8a/libmpfr.so"),
            layout.projectDirectory.file("src/main/jniLibs/arm64-v8a/libc++_shared.so"),
            layout.projectDirectory.file("src/main/jniLibs/arm64-v8a/.orca-mobile-engine.sha256")
        )
    }
    tasks.named("preBuild").configure { dependsOn(fetchOrcaEngine) }
