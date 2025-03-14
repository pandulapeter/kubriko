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

fun buildConfigurationValue(key: String) = project.findProperty("showcase.$key").toString()

fun isBuildConfigurationValueEnabled(key: String) = buildConfigurationValue(key) == "true"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.examples.gameWallbreaker)
            implementation(projects.examples.gameSpaceSquadron)
            implementation(projects.examples.gameAnnoyedPenguins)
            implementation(projects.examples.gameBlockysJourney)
            implementation(projects.examples.demoContentShaders)
            implementation(projects.examples.demoParticles)
            implementation(projects.examples.demoPerformance)
            implementation(projects.examples.demoPhysics)
            implementation(projects.examples.demoShaderAnimations)
            isBuildConfigurationValueEnabled("areTestExamplesEnabled").let { areTestExamplesEnabled ->
                implementation(if (areTestExamplesEnabled) projects.examples.testAudio else projects.examples.testAudioNoop)
                implementation(if (areTestExamplesEnabled) projects.examples.testCollision else projects.examples.testCollisionNoop)
                implementation(if (areTestExamplesEnabled) projects.examples.testInput else projects.examples.testInputNoop)
            }
            implementation(if (isBuildConfigurationValueEnabled("isDebugMenuEnabled")) projects.tools.debugMenu else projects.tools.debugMenuNoop)
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
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        versionCode = buildConfigurationValue("androidVersionCode").toInt()
        versionName = buildConfigurationValue("versionName")
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
            keyAlias = buildConfigurationValue("androidKeyAlias")
            keyPassword = buildConfigurationValue("androidKeyPassword")
            storeFile = file(buildConfigurationValue("androidKeystoreFile"))
            storePassword = buildConfigurationValue("androidKeystorePassword")
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
            packageName = "Kubriko Showcase"
            packageVersion = buildConfigurationValue("versionName")
            description = "Demo app showcasing the feature set of the Kubriko game engine"
            copyright = "© 2025 Pandula Péter. All rights reserved."
            macOS {
                targetFormats(TargetFormat.Dmg)
                iconFile.set(project.file("icon.icns"))
                bundleID = "com.pandulapeter.kubrikoShowcase"
                dockName = "Kubriko Showcase"
            }
            windows {
                targetFormats(TargetFormat.Exe)
                iconFile.set(project.file("icon.ico"))
                menuGroup = "Kubriko"
            }
            linux {
                targetFormats(TargetFormat.Deb)
                iconFile.set(project.file("icon.png"))
                debMaintainer = "pandulapeter@gmail.com"
                menuGroup = "kubriko-showcase"
            }
            buildTypes.release.proguard {
                configurationFiles.from(project.file("proguard-rules.pro"))
                isEnabled.set(true)
                optimize.set(true)
                obfuscate.set(true)
                version.set(libs.versions.proguard)
            }
        }
    }
}

buildkonfig {
    packageName = "com.pandulapeter.kubrikoShowcase"
    objectName = "BuildConfig"

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
        key: String,
    ) = buildConfigField(
        type = Type.BOOLEAN,
        name = name,
        value = buildConfigurationValue(key),
        const = true,
    )

    defaultConfigs {
        buildConfigFieldStringConstant(
            name = "LIBRARY_VERSION",
            value = rootProject.version.toString(),
        )
        buildConfigFieldStringConstant(
            name = "WEB_ROOT_PATH_NAME",
            value = buildConfigurationValue("webRootPathName"),
        )
        buildConfigFieldStringConstant(
            name = "APP_VERSION",
            value = buildConfigurationValue("versionName"),
        )
        buildConfigFieldBooleanConstant(
            name = "ARE_TEST_EXAMPLES_ENABLED",
            key = "areTestExamplesEnabled",
        )
        buildConfigFieldBooleanConstant(
            name = "IS_DEBUG_MENU_ENABLED",
            key = "isDebugMenuEnabled",
        )
        buildConfigFieldBooleanConstant(
            name = "IS_SCENE_EDITOR_ENABLED",
            key = "isSceneEditorEnabled",
        )
        buildConfigFieldBooleanConstant(
            name = "SHOULD_SHOW_UNFINISHED_GAMES",
            key = "shouldShowUnfinishedGames",
        )
    }
}