/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.buildLogic.plugins

import com.pandulapeter.kubriko.buildLogic.extensions.configurePublicArtifact
import com.pandulapeter.kubriko.buildLogic.extensions.libs
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

open class PublishingExtension {
    var artifactId: String? = null
}

class PublicArtifactPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("vanniktech-publish").get().get().pluginId)
        }
        val extension = project.extensions.create("artifactMetadata", PublishingExtension::class.java)
        project.afterEvaluate {
            extensions.configure<MavenPublishBaseExtension> {
                configurePublicArtifact(
                    extension = this,
                    artifactId = extension.artifactId ?: project.name,
                )
            }
        }
    }
}