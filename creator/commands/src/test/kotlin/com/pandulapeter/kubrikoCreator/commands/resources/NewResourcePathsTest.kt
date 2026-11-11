/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.commandContext
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NewResourcePathsTest {

    @Test
    fun aFileGoesToItsKindsFolderUnderItsOwnName() {
        assertEquals("drawable/ball.png", NewResourcePaths.of("ball.PNG", emptyList()))
        assertEquals("files/sounds/hit.wav", NewResourcePaths.of("hit.wav", emptyList()))
        assertEquals("files/music/Theme_1.ogg", NewResourcePaths.of("Theme_1.ogg", emptyList()))
        assertNull(NewResourcePaths.of("notes.txt", emptyList()))
        assertNull(NewResourcePaths.of("ball", emptyList()))
    }

    @Test
    fun aNameIsMadeOneTheRulesAllow() {
        listOf(
            "Hős lövés!.wav" to "files/sounds/Hos-loves.wav",
            "  big   boom  .wav" to "files/sounds/big-boom.wav",
            "_hidden.png" to "drawable/hidden.png",
            "ball.old.png" to "drawable/ball-old.png",
            "Игра.mp3" to "files/music/music.mp3",
            ".png" to "drawable/sprite.png",
        ).forEach { (fileName, path) ->
            assertEquals(path, NewResourcePaths.of(fileName, emptyList()), fileName)
            assertNull(ResourceKind.validate(path), path)
        }
    }

    @Test
    fun aNameTheGameWouldMixUpIsNumbered() {
        assertEquals("drawable/ball-2.png", NewResourcePaths.of("ball.png", listOf("drawable/ball.png")))
        assertEquals("drawable/ball-3.png", NewResourcePaths.of("ball.png", listOf("drawable/Ball.jpg", "drawable/ball_2.png")))
        assertEquals("files/sounds/hit.wav", NewResourcePaths.of("hit.wav", listOf("files/music/hit.mp3")))
    }

    @Test
    fun filesAddedTogetherGetNamesOfTheirOwn() = runTest {
        val context = commandContext(spriteFiles = SpriteFiles(mapOf("drawable/ball.png" to File("ball.png"))), audioFiles = AudioFiles())
        assertEquals(listOf("drawable/ball-2.png", "drawable/ball-3.png", null, "files/sounds/ball.wav"), context.newResourcePathsOf(listOf("ball.png", "ball.png", "ball.txt", "ball.wav")))
    }
}
