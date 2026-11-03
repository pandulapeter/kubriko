/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreatorSourcesTest {

    @Test
    fun findsTheAppAndEveryModule() {
        val creator = creatorDirectory().canonicalFile
        val roots = creatorSourceRoots().map { it.canonicalFile }
        assertTrue(File(creator, "src/main/kotlin") in roots, "$roots")
        assertTrue(File(creator, "foundation/src/main/kotlin") in roots, "$roots")
    }

    @Test
    fun findsTheTestsAndTestFixturesOfEveryModule() {
        val creator = creatorDirectory().canonicalFile
        val roots = creatorTestRoots().map { it.canonicalFile }
        assertTrue(File(creator, "src/test/kotlin") in roots, "$roots")
        assertTrue(File(creator, "foundation/src/test/kotlin") in roots, "$roots")
        assertTrue(File(creator, "foundation/src/testFixtures/kotlin") in roots, "$roots")
    }

    @Test
    fun failsWithoutTheProperty() {
        val directory = System.getProperty("kubrikoCreator.directory")
        System.clearProperty("kubrikoCreator.directory")
        try {
            val exception = assertFailsWith<IllegalStateException> { creatorSourceRoots() }
            assertEquals("Run through Gradle, which sets kubrikoCreator.directory.", exception.message)
        } finally {
            System.setProperty("kubrikoCreator.directory", directory)
        }
    }
}
