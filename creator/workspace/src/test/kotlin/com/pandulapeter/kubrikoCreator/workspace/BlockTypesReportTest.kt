/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockTypesReportTest {
    private val console = ConsoleLog()
    private val saved = mutableListOf<Boolean>()
    private val report = BlockTypesReport(console) { saved += it }

    @Test
    fun syncedBuildFilesReportTheVersionAndLibrariesEachTimeButANewerVersionOnce() {
        val changes = KubrikoBuildFileChanges(
            addedArtifacts = listOf("plugin-collision"),
            removedArtifacts = listOf("plugin-physics"),
            previousVersion = "1.0.0",
            version = "2.0.0",
            isNewerThanCreator = true,
        )
        report.onBuildFilesSynced(changes)
        report.onBuildFilesSynced(changes)
        val versionChanged = ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_VERSION_CHANGED, arguments = listOf("2.0.0", "1.0.0"))
        val librariesAdded = ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_LIBRARIES_ADDED, arguments = listOf("plugin-collision"))
        val librariesRemoved = ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_LIBRARIES_REMOVED, arguments = listOf("plugin-physics"))
        assertEquals(
            listOf(
                versionChanged,
                ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_VERSION_NEWER, arguments = listOf("2.0.0")),
                librariesAdded,
                librariesRemoved,
                versionChanged,
                librariesAdded,
                librariesRemoved,
            ),
            console.lines.value.entries,
        )
    }

    @Test
    fun theFirstLoadNamesEachTypeWhoseKotlinBlocksRelyOnWhatTheEventFunctionsChangedOnce() {
        val kotlin = { code: String -> EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin(code))) }
        report.onFirstLoaded(
            listOf(
                ActorTypeDefinition(name = "Ball", events = listOf(kotlin("return"), kotlin("if (score > 0f) return"))),
                ActorTypeDefinition(name = "Paddle", events = listOf(kotlin("velocity = SceneOffset.Zero"))),
            ),
        )
        assertEquals(
            listOf(ConsoleEntry.Status(WorkspaceStatus.KOTLIN_BLOCK_SCOPE_CHANGED, isError = true, arguments = listOf("Ball"))),
            console.lines.value.entries,
        )
    }

    @Test
    fun leftoversAreReportedWhenRemovedAndAsAnErrorWhenChangedByHand() {
        report.onLeftoversRemoved(listOf("a.kt", "b.kt"))
        report.onLeftoverChangedByHand("c.kt")
        assertEquals(
            listOf(
                ConsoleEntry.Status(WorkspaceStatus.GENERATED_LEFTOVERS_REMOVED, arguments = listOf("a.kt, b.kt")),
                ConsoleEntry.Status(WorkspaceStatus.GENERATED_LEFTOVER_CHANGED_BY_HAND, isError = true, arguments = listOf("c.kt")),
            ),
            console.lines.value.entries,
        )
    }

    @Test
    fun aSavedBatchIsHandedOnWithoutWritingToTheConsole() {
        report.onSaved(true)
        assertEquals(listOf(true), saved)
        assertEquals(emptyList(), console.lines.value.entries)
    }
}
