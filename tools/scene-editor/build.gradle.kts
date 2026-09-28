/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven

plugins {
    id("kubriko-compose-library")
    id("kubriko-public-artifact")
}

artifactMetadata {
    artifactId = "tool-scene-editor"
}

val isDebugMenuEnabled = project.findProperty("showcase.isDebugMenuEnabled") == "true"

kotlin {
    android {
        namespace = "com.pandulapeter.kubriko.sceneEditor"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.tools.sceneEditorApi)
            implementation(libs.compose.resources)
        }
        getByName("desktopMain") {
            dependencies {
                implementation(projects.plugins.collision)
                implementation(projects.plugins.keyboardInput)
                implementation(projects.plugins.persistence)
                implementation(projects.plugins.pointerInput)
                implementation(if (isDebugMenuEnabled) projects.tools.debugMenu else projects.tools.debugMenuNoop)
                implementation(projects.tools.uiComponents)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlin.reflect)
            }
        }
    }
}

if (!isDebugMenuEnabled) {
    tasks.withType<AbstractPublishToMaven>().configureEach {
        doFirst {
            throw GradleException("tool-scene-editor is published against tool-debug-menu: publish with showcase.isDebugMenuEnabled=true.")
        }
    }
}
