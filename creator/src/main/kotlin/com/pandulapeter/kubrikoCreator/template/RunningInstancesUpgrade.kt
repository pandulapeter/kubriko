/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.actors.ManagerTypeCodeGenerator

/**
 * Template version 8: `Game.kt` adds its Kubriko instance to the generated `KubrikoWiring.runningInstances` while it is
 * shown, and `GameEntryPoint` reports them to Kubriko Creator, so that `start_game` can wait until the game has drawn
 * its Actors. Since `start_game` only skips the wait without it, a file that doesn't look like the template's counts as
 * done rather than left to do by hand.
 */
internal object RunningInstancesUpgrade {
    private const val FIELD = "${ManagerTypeCodeGenerator.WIRING_NAME}.${ManagerTypeCodeGenerator.RUNNING_INSTANCES}"
    private const val DISPOSAL = "onDispose { kubriko.dispose() }"

    fun isGameDone(text: String) = FIELD in text || disposalOf(text.lines()) == null

    /**
     * [text] (a `Game.kt`) adding its instance while it is shown, or [text] itself if it does already or its
     * `DisposableEffect` doesn't look like the template's.
     */
    fun game(text: String): String {
        val lines = text.lines()
        if (FIELD in text) return text
        val disposal = disposalOf(lines) ?: return text
        val indent = lines[disposal].takeWhile { it == ' ' }
        val replacement = listOf("$indent$FIELD += kubriko", "${indent}onDispose {", "$indent    $FIELD -= kubriko", "$indent    kubriko.dispose()", "$indent}")
        return (lines.take(disposal) + replacement + lines.drop(disposal + 1)).joinToString("\n")
    }

    fun isEntryPointDone(text: String) = "getRunningInstances" in text || managersLineOf(text.lines()) == null

    /**
     * [text] (a `GameEntryPoint.kt`) reporting the running instances, or [text] itself if it does already or doesn't hand
     * the Scene editor its Managers in one line, as the template does.
     */
    fun entryPoint(text: String): String {
        val lines = text.lines()
        if ("getRunningInstances" in text) return text
        val line = managersLineOf(lines) ?: return text
        val indent = lines[line].takeWhile { it == ' ' }
        return (lines.take(line + 1) + listOf("", "${indent}override fun getRunningInstances() = $FIELD") + lines.drop(line + 1)).joinToString("\n")
    }

    private fun disposalOf(lines: List<String>) = lines.indices.filter { lines[it].trim() == DISPOSAL }.singleOrNull()

    private fun managersLineOf(lines: List<String>) =
        lines.indices.filter { lines[it].trimStart().startsWith("override fun createCustomManagers()") && lines[it].trimEnd().endsWith(")") }.singleOrNull()
}
