/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.buildLogic.plugins

import com.android.build.api.dsl.ApplicationExtension
import com.pandulapeter.kubriko.buildLogic.extensions.configureKotlinAndroid
import com.pandulapeter.kubriko.buildLogic.extensions.configureKotlinMultiplatform
import com.pandulapeter.kubriko.buildLogic.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ApplicationPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("kotlin-multiplatform").get().get().pluginId)
            apply(libs.findPlugin("androidApplication").get().get().pluginId)
            apply(libs.findPlugin("compose").get().get().pluginId)
            apply(libs.findPlugin("compose-compiler").get().get().pluginId)
        }
        extensions.configure<KotlinMultiplatformExtension>(::configureKotlinMultiplatform)
        extensions.configure<ApplicationExtension>(::configureKotlinAndroid)
    }
}