/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.buildLogic.extensions

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

internal fun Project.configureKotlinMultiplatform(
    extension: KotlinMultiplatformExtension
) = extension.apply {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    extension.configure<KotlinMultiplatformAndroidLibraryTarget> {
        minSdk = libs.findVersion("android-minSdk").get().toString().toInt()
        compileSdk = libs.findVersion("android-compileSdk").get().toString().toInt()
        androidResources.enable = true
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    sourceSets.getByName("commonTest").dependencies {
        implementation(libs.findLibrary("kotlin-test").get())
        implementation(libs.findLibrary("kotlinx-coroutines-test").get())
    }
    if (path != ":tools:test-fixtures") {
        sourceSets.getByName("desktopTest").dependencies {
            implementation(project(":tools:test-fixtures"))
        }
    }
    // Running these needs a browser or an iOS simulator; their compilations still run, so commonTest must compile everywhere.
    tasks.withType(KotlinJsTest::class.java).configureEach { enabled = false }
    tasks.withType(KotlinNativeTest::class.java).configureEach { enabled = false }
    // Libraries declare no Wasm executable, which this Compose check demands only for the browser test runs disabled above.
    tasks.matching { it.name == "checkComposeUiTestConfigurationForWasmJs" }.configureEach { enabled = false }
}
