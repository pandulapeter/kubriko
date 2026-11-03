/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.buildLogic.plugins

import com.pandulapeter.kubriko.buildLogic.extensions.libs
import com.pandulapeter.kubriko.buildLogic.extensions.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * The system property that tells a Kubriko Creator test where the `creator` folder is, for the tests that read the sources of
 * every module.
 */
const val CREATOR_DIRECTORY_PROPERTY = "kubrikoCreator.directory"

/**
 * A module of Kubriko Creator (a plain JVM library the `:creator` app depends on): Kotlin with serialization, the style check,
 * test fixtures, and explicit API mode, so that what a module offers the others is written `public` on purpose.
 */
class CreatorModulePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply(libs.pluginId("kotlin-desktop"))
            apply(libs.pluginId("kotlin-serialization"))
            apply("java-test-fixtures")
        }
        pluginManager.apply(StylePlugin::class.java)
        extensions.configure<KotlinJvmProjectExtension> { explicitApi() }
        extensions.configure<BasePluginExtension> { archivesName.set("kubriko-creator-${project.name}") }
        dependencies {
            add("implementation", libs.findLibrary("kotlinx-coroutines").get())
            add("implementation", libs.findLibrary("kotlinx-serialization").get())
            add("testImplementation", libs.findLibrary("kotlin-test").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            // A module's own implementation dependencies don't reach its test fixtures (they see only its API).
            listOf("kotlinx-coroutines", "kotlinx-serialization", "kotlin-test", "kotlinx-coroutines-test").forEach {
                add("testFixturesImplementation", libs.findLibrary(it).get())
            }
        }
        tasks.withType<Test>().configureEach {
            systemProperty(CREATOR_DIRECTORY_PROPERTY, rootProject.file("creator").absolutePath)
        }
    }
}
