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

import com.diffplug.gradle.spotless.SpotlessExtension
import com.pandulapeter.kubriko.buildLogic.extensions.libs
import com.pandulapeter.kubriko.buildLogic.extensions.pluginId
import com.pandulapeter.kubriko.buildLogic.extensions.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * ktlint through Spotless (`spotlessCheck`, `spotlessApply`), with the rules the root `.editorconfig` leaves enabled.
 * The library convention plugins apply it; every other module applies it on its own rather than from a root
 * `subprojects {}` block, which would be cross-project configuration. The Kotlin sources are found by path, so every
 * source set of every kind of module is covered without asking a Kotlin or Android plugin for its source sets.
 */
class StylePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(libs.pluginId("spotless"))
        val ktlintVersion = libs.version("ktlint")
        extensions.configure<SpotlessExtension> {
            kotlin {
                target("src/**/*.kt")
                ktlint(ktlintVersion)
            }
            kotlinGradle {
                target("*.gradle.kts")
                ktlint(ktlintVersion)
            }
        }
    }
}
