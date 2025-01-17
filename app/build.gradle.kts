/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("kubriko-application")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.examples.demoAudio)
            implementation(projects.examples.demoContentShaders)
            implementation(projects.examples.demoInput)
            implementation(projects.examples.demoPerformance)
            implementation(projects.examples.demoPhysics)
            implementation(projects.examples.demoShaderAnimations)
            implementation(projects.examples.gameSpaceSquadron)
            implementation(projects.examples.gameWallbreaker)
            implementation(projects.tools.debugMenu)
            implementation(projects.tools.uiComponents)
            implementation(compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashScreen)
            implementation(libs.google.material)
        }
        val desktopMain by getting {
            dependencies {
                implementation(projects.engine)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "com.pandulapeter.kubrikoShowcase"
    defaultConfig {
        applicationId = "com.pandulapeter.kubrikoShowcase"
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = System.getProperty("SHOWCASE_ANDROID_VERSION_CODE").toInt()
        versionName = System.getProperty("SHOWCASE_ANDROID_VERSION_NAME")
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
            keyAlias = System.getProperty("SHOWCASE_ANDROID_KEY_ALIAS")
            keyPassword = System.getProperty("SHOWCASE_ANDROID_KEY_PASSWORD")
            storeFile = file(System.getProperty("SHOWCASE_ANDROID_STORE_FILE"))
            storePassword = System.getProperty("SHOWCASE_ANDROID_STORE_PASSWORD")
        }
    }
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
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
}

compose.desktop {
    application {
        mainClass = "com.pandulapeter.kubrikoShowcase.KubrikoShowcaseAppKt"
        nativeDistributions {
            packageName = "com.pandulapeter.kubrikoShowcase"
            packageVersion = System.getProperty("SHOWCASE_DEKTOP_VERSION_NAME")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            macOS { iconFile.set(project.file("icon.icns")) }
            windows { iconFile.set(project.file("icon.ico")) }
            linux { iconFile.set(project.file("icon.png")) }
            buildTypes.release.proguard {
                configurationFiles.from(project.file("proguard-rules.pro"))
                isEnabled.set(true)
                obfuscate.set(true)
            }
        }
    }
}
