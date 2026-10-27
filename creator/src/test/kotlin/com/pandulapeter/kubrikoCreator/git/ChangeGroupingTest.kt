/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.git

import kotlin.test.Test
import kotlin.test.assertEquals

class ChangeGroupingTest {
    private val generated = "src/commonMain/kotlin/game/generated"

    @Test
    fun anActorTypesDefinitionAndGeneratedCodeAreOneChange() {
        assertEquals(
            listOf(ChangeEntry("Ball", ChangeKind.MODIFIED, listOf(".kubriko/actors/Ball.json", "$generated/Ball.kt"), isBlockType = true)),
            ChangeGrouping.group(
                listOf(FileChange(".kubriko/actors/Ball.json", ChangeKind.MODIFIED), FileChange("$generated/Ball.kt", ChangeKind.MODIFIED)),
                generated,
            ),
        )
    }

    @Test
    fun actorTypesComeFirstAlphabeticallyThenFiles() {
        val entries = ChangeGrouping.group(
            listOf(
                FileChange("README.md", ChangeKind.MODIFIED),
                FileChange(".kubriko/actors/Wall.json", ChangeKind.ADDED),
                FileChange(".kubriko/actors/Ball.json", ChangeKind.MODIFIED),
                FileChange("$generated/GeneratedActorTypes.kt", ChangeKind.MODIFIED),
            ),
            generated,
        )
        assertEquals(listOf("Ball", "Wall", "README.md", "$generated/GeneratedActorTypes.kt"), entries.map { it.title })
        assertEquals(listOf(true, true, false, false), entries.map { it.isBlockType })
    }

    @Test
    fun aNewActorTypeIsAddedAndAMixedOneModified() {
        val entries = ChangeGrouping.group(
            listOf(
                FileChange(".kubriko/actors/Wall.json", ChangeKind.ADDED),
                FileChange("$generated/Wall.kt", ChangeKind.ADDED),
                FileChange(".kubriko/actors/Ball.json", ChangeKind.MODIFIED),
                FileChange("$generated/Ball.kt", ChangeKind.DELETED),
            ),
            generated,
        )
        assertEquals(mapOf("Ball" to ChangeKind.MODIFIED, "Wall" to ChangeKind.ADDED), entries.associate { it.title to it.kind })
    }

    @Test
    fun aConflictWinsOverEveryOtherKind() {
        val entries = ChangeGrouping.group(
            listOf(FileChange(".kubriko/actors/Ball.json", ChangeKind.CONFLICTING), FileChange("$generated/Ball.kt", ChangeKind.MODIFIED)),
            generated,
        )
        assertEquals(ChangeKind.CONFLICTING, entries.single().kind)
    }

    @Test
    fun withoutGeneratedCodeOnlyDefinitionsAreGrouped() {
        val entries = ChangeGrouping.group(
            listOf(FileChange("src/Ball.kt", ChangeKind.MODIFIED), FileChange(".kubriko/actors/nested/Ball.json", ChangeKind.MODIFIED)),
            generatedPath = null,
        )
        assertEquals(listOf(false, false), entries.map { it.isBlockType })
    }
}
