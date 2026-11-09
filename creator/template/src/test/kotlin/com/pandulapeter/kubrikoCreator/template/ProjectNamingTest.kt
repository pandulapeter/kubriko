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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProjectNamingTest {

    @Test
    fun namesStartWithALetterAndUseOnlySafeCharacters() {
        listOf("Game", "My Game 2", "space_shooter", "Kubriko-Demo", "  Padded  ", "Café", "Kalandjáték", "Kalandja\u0301te\u0301k", "Игра", "冒険", "गाना", "\uD840\uDC00\uD840\uDC01").forEach {
            assertNull(ProjectNaming.validateName(it), it)
        }
        assertEquals(TemplateProblem.NAME_EMPTY, ProjectNaming.validateName("   "))
        listOf("2048", "-dash", "_game").forEach { assertEquals(TemplateProblem.NAME_FIRST_CHARACTER, ProjectNaming.validateName(it), it) }
        listOf("My \"Game\"", "a/b", "a\\b", "Game: Reloaded", "Game\uDB40\uDC20", "Game\uDB80\uDC5F", "Game \u0301").forEach { assertEquals(TemplateProblem.NAME_CHARACTERS, ProjectNaming.validateName(it), it) }
    }

    @Test
    fun packagesNeedTwoLowercaseSegmentsThatArentKeywords() {
        listOf("com.example.game", "io.github.me.game_2").forEach { assertNull(ProjectNaming.validatePackageName(it), it) }
        mapOf(
            "" to TemplateProblem.PACKAGE_NAME_PARTS,
            "game" to TemplateProblem.PACKAGE_NAME_PARTS,
            "com.Example" to TemplateProblem.PACKAGE_NAME_FIRST_CHARACTER,
            "com.exAmple" to TemplateProblem.PACKAGE_NAME_CHARACTERS,
            "com..game" to TemplateProblem.PACKAGE_NAME_PART_EMPTY,
            "com.example." to TemplateProblem.PACKAGE_NAME_PART_EMPTY,
            "com.2game" to TemplateProblem.PACKAGE_NAME_FIRST_CHARACTER,
            "com.example.fun" to TemplateProblem.PACKAGE_NAME_KEYWORD,
            "com.example.my-game" to TemplateProblem.PACKAGE_NAME_CHARACTERS,
            "com.example.kalandjáték" to TemplateProblem.PACKAGE_NAME_LETTER_BEYOND_A_TO_Z,
        ).forEach { (packageName, problem) -> assertEquals(problem, ProjectNaming.validatePackageName(packageName), packageName) }
    }

    @Test
    fun aPackageNamedLikeAKeywordIsRejected() {
        assertEquals(TemplateProblem.PACKAGE_NAME_KEYWORD, ProjectNaming.validatePackageName("com.typealias"))
    }

    @Test
    fun theProjectIdIsTheNameInLowercaseKebabCase() {
        assertEquals("my-game", ProjectNaming.projectId("My Game"))
        assertEquals("space-shooter-2", ProjectNaming.projectId("  Space__Shooter - 2 "))
    }

    @Test
    fun theProjectIdFoldsAccentsAndStartsWithALetter() {
        mapOf(
            "Kalandjáték" to "kalandjatek",
            "Kalandja\u0301te\u0301k" to "kalandjatek",
            "Straße Ærø" to "strasse-aero",
            "Café 2" to "cafe-2",
            "Игра" to "game",
            "Игра 2" to "game-2",
            "冒険 Quest" to "quest",
        ).forEach { (name, id) -> assertEquals(id, ProjectNaming.projectId(name), name) }
    }

    @Test
    fun theDefaultPackageIsAlwaysValid() {
        listOf("My Game" to "com.example.mygame", "2048 Clone" to "com.example.clone", "Fun" to "com.example.fungame", "!!!" to "com.example.game", "Kalandjáték" to "com.example.kalandjatek", "Игра" to "com.example.game").forEach { (name, expected) ->
            assertEquals(expected, ProjectNaming.defaultPackageName(name))
            assertNull(ProjectNaming.validatePackageName(ProjectNaming.defaultPackageName(name)))
        }
    }
}
