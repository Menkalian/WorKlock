import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinKapt)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.velaKeygen)
}

android {
    namespace = "de.menkalian.worklock"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.menkalian.worklock"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            signingConfig = signingConfigs.create("release") {
                storeFile(rootProject.file("keystore/keystore.jks"))
                storePassword("DR%dz2LhNg#CCHM8SgrvZ5BY&sySvL")
                keyAlias("key0")
                keyPassword("8cX187WL1&BLYTbjp^Xvpf2F^R^hnj")

                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST,io.netty.versions.properties}"
            excludes += "/org/sqlite/native/**"
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.core.ktx)
    implementation(libs.datastore)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))

    implementation(libs.hilt.android)
    implementation(libs.hilt.ext.navigationcompose)
    kapt(libs.hilt.compiler)

    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.ui.navigation.compose)
    implementation(libs.ui.calendar)
    implementation(libs.ui.wheel)
    implementation(libs.material3)

    implementation(libs.exposed.core)
    implementation(libs.exposed.javatime)
    implementation(libs.exposed.jdbc)
    implementation(libs.sqlite)
    implementation(libs.slf4j.android)

    implementation(libs.vela.transfervalue)
    implementation(libs.poi.core)
    implementation(libs.poi.ooxml)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}

keygen {
    create("default") {
        sourceDir = project.mkdir("src/ckf")
        finalLayerAsString = true
    }
}

tasks.withType(KotlinCompile::class).configureEach {
    this.dependsOn(tasks.generateKeyObjects)
}
