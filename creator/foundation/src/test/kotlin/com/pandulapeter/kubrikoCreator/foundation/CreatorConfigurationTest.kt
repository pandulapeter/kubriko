/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import com.pandulapeter.kubrikoCreator.testing.testConfiguration
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreatorConfigurationTest {
    private val bundled = properties(
        "kubrikoVersion" to "1.0.0", "kotlinVersion" to "2.4.21", "composeVersion" to "1.12.1", "gradleVersion" to "9.7.1",
        "androidGradlePluginVersion" to "9.5.0", "androidCompileSdk" to "37", "androidMinSdk" to "29", "androidxActivityVersion" to "1.13.0",
    )

    @Test
    fun theBundledVersionsAreUsedByDefault() {
        assertEquals(
            testConfiguration(),
            CreatorConfiguration.from(bundled, Properties()),
        )
    }

    @Test
    fun systemPropertiesSwitchToUnreleasedModules() {
        val configuration = CreatorConfiguration.from(
            bundled,
            properties("kubriko.creator.kubrikoVersion" to "1.0.1-dev", "kubriko.creator.useMavenLocal" to "true"),
        )
        assertEquals("1.0.1-dev", configuration.kubrikoVersion)
        assertTrue(configuration.useMavenLocal)
    }

    @Test
    fun aDevelopmentBuildBundlesMavenLocal() {
        bundled.setProperty("useMavenLocal", "true")
        assertTrue(CreatorConfiguration.from(bundled, Properties()).useMavenLocal)
    }

    @Test
    fun aMissingBundledVersionFailsLoudly() {
        assertFailsWith<IllegalStateException> { CreatorConfiguration.from(properties("kubrikoVersion" to "1.0.0"), Properties()) }
    }

    @Test
    fun theBuildBundlesEveryVersion() {
        val configuration = CreatorConfiguration.load()
        listOf(configuration.kubrikoVersion, configuration.kotlinVersion, configuration.composeVersion, configuration.gradleVersion, configuration.android.gradlePlugin, configuration.android.activity).forEach {
            assertTrue(it.matches(Regex("\\d+\\.\\d+.*")), it)
        }
        assertTrue(configuration.android.minSdk in 21..configuration.android.compileSdk)
    }

    private fun properties(vararg entries: Pair<String, String>) = Properties().apply { entries.forEach { (key, value) -> setProperty(key, value) } }
}
