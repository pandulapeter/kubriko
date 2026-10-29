/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpectDeclarationsTest {
    private val root = createTempDirectory().toFile()
    private val declarations = ExpectDeclarations(root)

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun declarationsOfEveryKindAreFound() {
        listOf(
            "expect fun platformName(): String",
            "internal expect class Clock()",
            "    public expect object Platform",
            "expect abstract class Base",
            "@OptionalExpectation expect annotation class Marker",
            "expect val isDebug: Boolean",
            "expect interface Logger",
            "expect typealias Lock",
        ).forEach { assertTrue(ExpectDeclarations.declaresExpect("package a\n\n$it\n"), it) }
    }

    @Test
    fun actualsCommentsAndOtherUsesOfTheWordArent() {
        listOf(
            "actual fun platformName() = \"JVM\"",
            "// expect fun platformName(): String",
            " * expect class Clock",
            "val expected = expect(1)",
            "fun expect(value: Int) = value",
        ).forEach { assertFalse(ExpectDeclarations.declaresExpect("package a\n\n$it\n"), it) }
    }

    @Test
    fun everySourceFileCountsAndChangesAreNoticed() {
        val source = root.resolve("src/commonMain/kotlin/Platform.kt").apply { parentFile.mkdirs() }
        source.writeText("fun name() = \"\"\n")
        root.resolve("src/desktopMain/kotlin/Main.kt").apply { parentFile.mkdirs() }.writeText("fun main() = Unit\n")
        assertFalse(declarations.any())
        source.writeText("expect fun name(): String\n")
        source.setLastModified(source.lastModified() + 2_000)
        assertTrue(declarations.any())
        source.delete()
        assertFalse(declarations.any())
    }

    @Test
    fun theSourcesOfEveryModuleCountButNotWhatTheBuildsWrite() {
        root.resolve("composeApp/build/generated/Platform.kt").apply { parentFile.mkdirs() }.writeText("expect fun name(): String\n")
        assertFalse(declarations.any())
        root.resolve("composeApp/src/commonMain/kotlin/Platform.kt").apply { parentFile.mkdirs() }.writeText("expect fun name(): String\n")
        assertTrue(declarations.any())
    }

    @Test
    fun aProjectWithoutSourcesDeclaresNone() {
        assertFalse(declarations.any())
    }
}
