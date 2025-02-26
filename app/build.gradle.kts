/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import com.codingfeline.buildkonfig.gradle.TargetConfigDsl
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("kubriko-application")
    alias(libs.plugins.codingfeline.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.examples.demoContentShaders)
            implementation(projects.examples.demoParticles)
            implementation(projects.examples.demoPhysics)
            implementation(projects.examples.demoShaderAnimations)
            implementation(projects.examples.gameAnnoyedPenguins)
            implementation(projects.examples.gameSpaceSquadron)
            implementation(projects.examples.gameWallbreaker)
            implementation(projects.examples.testAudio)
            implementation(projects.examples.testInput)
            implementation(projects.examples.testPerformance)
            implementation(projects.tools.debugMenu)
            implementation(projects.tools.uiComponents)
            implementation(compose.components.resources)
            implementation(libs.compose.backhandler)
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

buildkonfig {
    packageName = "com.pandulapeter.kubrikoShowcase"
    objectName = "BuildConfig"

    val libraryVersionConstantName = "LIBRARY_VERSION"
    val areTestExamplesEnabledConstantName = "ARE_TEST_EXAMPLES_ENABLED"
    val isDebugMenuEnabledConstantName = "IS_DEBUG_MENU_ENABLED"
    val isSceneEditorEnabledConstantName = "IS_SCENE_EDITOR_ENABLED"

    fun TargetConfigDsl.buildConfigFieldStringConstant(
        name: String,
        value: String,
    ) = buildConfigField(
        type = Type.STRING,
        name = name,
        value = value,
        const = true,
    )

    fun TargetConfigDsl.buildConfigFieldBooleanConstant(
        name: String,
        value: Boolean,
    ) = buildConfigField(
        type = Type.BOOLEAN,
        name = name,
        value = if (value) "true" else "false",
        const = true,
    )

    defaultConfigs {
        buildConfigFieldStringConstant(
            name = libraryVersionConstantName,
            value = rootProject.version.toString(),
        )
        buildConfigFieldBooleanConstant(
            name = areTestExamplesEnabledConstantName,
            value = true,
        )
        buildConfigFieldBooleanConstant(
            name = isDebugMenuEnabledConstantName,
            value = true,
        )
        buildConfigFieldBooleanConstant(
            name = isSceneEditorEnabledConstantName,
            value = true,
        )
    }
    defaultConfigs("release") {
        buildConfigFieldBooleanConstant(
            name = areTestExamplesEnabledConstantName,
            value = false,
        )
        buildConfigFieldBooleanConstant(
            name = isDebugMenuEnabledConstantName,
            value = false,
        )
        buildConfigFieldBooleanConstant(
            name = isSceneEditorEnabledConstantName,
            value = false,
        )
    }
}