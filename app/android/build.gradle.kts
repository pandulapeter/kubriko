import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */

val localProperties = Properties().also { properties ->
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(properties::load)
}

fun findProperty(key: String) = localProperties.getProperty(key) ?: project.findProperty(key).toString()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.app.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashScreen)
    implementation(libs.google.material)
}

android {
    namespace = "com.pandulapeter.kubrikoShowcase"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "com.pandulapeter.kubrikoShowcase"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        versionCode = project.findProperty("showcase.androidVersionCode").toString().toInt()
        versionName = project.findProperty("showcase.versionName").toString()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    val internalSigningConfig = "internal"
    val releaseSigningConfig = "release"
    signingConfigs {
        create(internalSigningConfig) {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = file("internal.keystore")
            storePassword = "android"
        }
        create(releaseSigningConfig) {
            keyAlias = findProperty("showcase.androidKeyAlias")
            keyPassword = findProperty("showcase.androidKeyPassword")
            storeFile = file(findProperty("showcase.androidKeystoreFile"))
            storePassword = findProperty("showcase.androidKeystorePassword")
        }
    }
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            versionNameSuffix = "-debug"
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName(internalSigningConfig)
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(releaseSigningConfig)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}