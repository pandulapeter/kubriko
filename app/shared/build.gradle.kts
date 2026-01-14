/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
import com.android.build.api.dsl.androidLibrary
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import com.codingfeline.buildkonfig.gradle.TargetConfigDsl

plugins {
    id("kubriko-compose-library")
    alias(libs.plugins.codingfeline.buildkonfig)
}

fun buildConfigurationValue(key: String) = project.findProperty("showcase.$key").toString()

fun isBuildConfigurationValueEnabled(key: String) = buildConfigurationValue(key) == "true"

kotlin {
    androidLibrary {
        namespace = "com.pandulapeter.kubrikoShowcase"
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            implementation(projects.examples.gameWallbreaker)
            implementation(projects.examples.gameSpaceSquadron)
            implementation(projects.examples.gameAnnoyedPenguins)
            implementation(projects.examples.gameBlockysJourney)
            implementation(projects.examples.demoContentShaders)
            implementation(projects.examples.demoIsometricGraphics)
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
            implementation(libs.compose.resources)
            implementation(libs.compose.backHandler)
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