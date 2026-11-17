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

import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class GameResourcesTest {
    private val files = SpriteFiles(mapOf("drawable/ball.png" to File("ball.png"), "drawable/icon.png" to File("icon.png")))

    @Test
    fun onlyTheGameModulesOwnSpritesAreOfferedOnceTheScanIsKnown() {
        assertEquals(listOf("drawable/ball.png"), GameResources(sprites = setOf("drawable/ball.png")).spritesAmong(files).paths)
        assertEquals(files, GameResources().spritesAmong(files))
    }
}
