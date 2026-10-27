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

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_package_name_invalid
import kubriko.creator.generated.resources.error_project_name_empty
import kubriko.creator.generated.resources.error_project_name_invalid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProjectNamingTest {

    @Test
    fun namesStartWithALetterAndUseOnlySafeCharacters() {
        listOf("Game", "My Game 2", "space_shooter", "Kubriko-Demo", "  Padded  ").forEach { assertNull(ProjectNaming.validateName(it), it) }
        assertEquals(Res.string.error_project_name_empty, ProjectNaming.validateName("   "))
        listOf("2048", "My \"Game\"", "Café", "a/b", "-dash").forEach { assertEquals(Res.string.error_project_name_invalid, ProjectNaming.validateName(it), it) }
    }

    @Test
    fun packagesNeedTwoLowercaseSegmentsThatArentKeywords() {
        listOf("com.example.game", "io.github.me.game_2").forEach { assertNull(ProjectNaming.validatePackageName(it), it) }
        listOf("game", "com.Example", "com..game", "com.example.", "com.2game", "com.example.fun", "com.example.my-game").forEach {
            assertEquals(Res.string.error_package_name_invalid, ProjectNaming.validatePackageName(it), it)
        }
    }

    @Test
    fun theProjectIdIsTheNameInLowercaseKebabCase() {
        assertEquals("my-game", ProjectNaming.projectId("My Game"))
        assertEquals("space-shooter-2", ProjectNaming.projectId("  Space__Shooter - 2 "))
    }

    @Test
    fun theDefaultPackageIsAlwaysValid() {
        listOf("My Game" to "com.example.mygame", "2048 Clone" to "com.example.clone", "Fun" to "com.example.fungame", "!!!" to "com.example.game").forEach { (name, expected) ->
            assertEquals(expected, ProjectNaming.defaultPackageName(name))
            assertNull(ProjectNaming.validatePackageName(ProjectNaming.defaultPackageName(name)))
        }
    }
}
