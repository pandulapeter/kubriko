/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.scenes.SceneNaming
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NameFlawTest {

    private fun flawOf(name: String) = NameFlaw.of(name, isFirstAllowed = { it in 'a'..'z' }, isAllowed = NameFlaw::isLetterOrDigitAToZ)

    @Test
    fun aNameHasTheFirstFlawItShows() {
        assertNull(flawOf("spawn2"))
        assertEquals(NameFlaw.EMPTY, flawOf(""))
        assertEquals(NameFlaw.FIRST_CHARACTER, flawOf("Spawn"))
        assertEquals(NameFlaw.CHARACTER, flawOf("spawn enemy"))
        assertEquals(NameFlaw.CHARACTER, flawOf("spawn٣"))
    }

    @Test
    fun lettersBeyondAToZAreAFlawOfTheirOwnWhereverTheyAre() {
        listOf("élet", "Kalandjáték", "straße", "ninja忍者", "e\u0301let", "gaana\u093E", "a\uD840\uDC00").forEach { assertEquals(NameFlaw.LETTER_BEYOND_A_TO_Z, flawOf(it), it) }
        assertEquals(NameFlaw.CHARACTER, flawOf("game\uD83C\uDFAE"))
        assertEquals(NameFlaw.CHARACTER, flawOf("game \u20DD"))
        listOf("ball\uFE0F", "ball\u034F", "ball\uDB40\uDD00").forEach { assertEquals(NameFlaw.CHARACTER, flawOf(it), it) }
    }

    @Test
    fun aFolderIsRefusedForTheRuleItBreaks() {
        assertNull(FolderPaths.validateName("Flying enemies_2"))
        assertEquals(NameProblem.NAME_EMPTY, FolderPaths.validateName(""))
        assertEquals(NameProblem.FOLDER_FIRST_CHARACTER, FolderPaths.validateName(" enemies"))
        assertEquals(NameProblem.FOLDER_CHARACTERS, FolderPaths.validateName("enemies!"))
        assertEquals(NameProblem.FOLDER_END_SPACE, FolderPaths.validateName("enemies "))
        assertEquals(NameProblem.NAME_LETTER_BEYOND_A_TO_Z, FolderPaths.validateName("ellenségek"))
        assertEquals(NameProblem.PATH_PART_EMPTY, FolderPaths.validatePath("enemies/"))
    }

    @Test
    fun aSceneIsRefusedForTheRuleItBreaks() {
        assertNull(SceneNaming.validate("levels/1-1"))
        assertEquals(NameProblem.NAME_EMPTY, SceneNaming.validate(""))
        assertEquals(NameProblem.PATH_PART_EMPTY, SceneNaming.validate("levels//1"))
        assertEquals(NameProblem.SCENE_FIRST_CHARACTER, SceneNaming.validate("levels/-1"))
        assertEquals(NameProblem.SCENE_CHARACTERS, SceneNaming.validate("level 1"))
        assertEquals(NameProblem.NAME_LETTER_BEYOND_A_TO_Z, SceneNaming.validate("pálya-1"))
    }

    @Test
    fun aPageIsRefusedForTheRuleItBreaks() {
        assertNull(PageNaming.validate("worlds/the cave"))
        assertEquals(NameProblem.NAME_EMPTY to emptyList(), PageNaming.validate(""))
        assertEquals(NameProblem.FOLDER_CHARACTERS to emptyList(), PageNaming.validate("hero.md"))
        assertNull(PageNaming.validate("a".repeat(120)))
        assertEquals(NameProblem.PAGE_NAME_TOO_LONG to listOf("120"), PageNaming.validate("a".repeat(121)))
    }
}
