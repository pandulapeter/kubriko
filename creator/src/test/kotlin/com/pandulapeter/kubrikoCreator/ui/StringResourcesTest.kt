/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class StringResourcesTest {
    private val strings = File("src/main/composeResources/values/strings.xml").readText()

    @Test
    fun apostrophesAreWrittenPlainBecauseComposeResourcesShowEscapesLiterally() {
        val escaped = strings.lines().filter { "\\'" in it || "\\\"" in it }
        assertTrue(escaped.isEmpty(), escaped.joinToString("\n"))
    }

    @Test
    fun everyStringAndIconIsUsed() {
        val sources = File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        val unusedStrings = Regex("<string name=\"([a-z0-9_]+)\"").findAll(strings).map { it.groupValues[1] }.filter { "Res.string.$it" !in sources }.toList()
        val icons = File("src/main/composeResources/drawable")
        assertTrue(icons.isDirectory, "Run from the creator folder: ${icons.absolutePath}")
        val unusedIcons = icons.list().orEmpty().map { it.substringBeforeLast('.') }.filter { "Res.drawable.$it" !in sources }
        assertTrue(unusedStrings.isEmpty(), "Unused strings: $unusedStrings")
        assertTrue(unusedIcons.isEmpty(), "Unused icons: $unusedIcons")
    }
}
