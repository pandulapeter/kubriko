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
import kotlin.test.assertTrue

/**
 * Checks the resources of the Kubriko Creator [module] (its folder): every string of its `strings.xml` and every icon of its
 * `drawable` folder is used by its own sources, and no string escapes an apostrophe or a quote, since Compose resources show
 * escapes literally.
 */
public fun assertEveryStringAndIconIsUsed(module: File) {
    val resources = File(module, "src/main/composeResources")
    val strings = File(resources, "values/strings.xml")
    assertTrue(strings.isFile, "No strings at ${strings.absolutePath}")
    val stringsText = strings.readText()
    val escaped = stringsText.lines().filter { "\\'" in it || "\\\"" in it }
    assertTrue(escaped.isEmpty(), escaped.joinToString("\n"))
    val sources = File(module, "src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
    val unusedStrings = Regex("<string name=\"([a-z0-9_]+)\"").findAll(stringsText).map { it.groupValues[1] }.filter { "Res.string.$it" !in sources }.toList()
    val icons = File(resources, "drawable")
    assertTrue(icons.isDirectory, "No icons at ${icons.absolutePath}")
    val unusedIcons = icons.list().orEmpty().map { it.substringBeforeLast('.') }.filter { "Res.drawable.$it" !in sources }
    assertTrue(unusedStrings.isEmpty(), "Unused strings: $unusedStrings")
    assertTrue(unusedIcons.isEmpty(), "Unused icons: $unusedIcons")
}
