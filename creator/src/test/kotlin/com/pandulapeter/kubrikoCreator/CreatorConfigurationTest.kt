/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator

import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreatorConfigurationTest {
    private val bundled = properties("kubrikoVersion" to "1.0.0", "kotlinVersion" to "2.4.21", "composeVersion" to "1.12.1", "gradleVersion" to "9.7.1")

    @Test
    fun theBundledVersionsAreUsedByDefault() {
        assertEquals(
            CreatorConfiguration(kubrikoVersion = "1.0.0", kotlinVersion = "2.4.21", composeVersion = "1.12.1", gradleVersion = "9.7.1", useMavenLocal = false),
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
    fun aMissingBundledVersionFailsLoudly() {
        assertFailsWith<IllegalStateException> { CreatorConfiguration.from(properties("kubrikoVersion" to "1.0.0"), Properties()) }
    }

    @Test
    fun theBuildBundlesEveryVersion() {
        val configuration = CreatorConfiguration.load()
        listOf(configuration.kubrikoVersion, configuration.kotlinVersion, configuration.composeVersion, configuration.gradleVersion).forEach {
            assertTrue(it.matches(Regex("\\d+\\.\\d+.*")), it)
        }
    }

    private fun properties(vararg entries: Pair<String, String>) = Properties().apply { entries.forEach { (key, value) -> setProperty(key, value) } }
}
