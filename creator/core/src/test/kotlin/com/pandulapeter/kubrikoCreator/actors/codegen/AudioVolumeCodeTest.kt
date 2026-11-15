/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioVolumeCodeTest {
    private val scope = BlockScope("Ball", properties = listOf(PropertyDefinition.Number("loudness")))

    private fun line(block: BlockDefinition) = BlockCodeGenerator.blockCode(block, EventType.EVERY_FRAME, scope).lines.single()

    @Test
    fun fullVolumeGeneratesWhatItDidBeforeAndAnyOtherIsAFraction() {
        assertEquals("generatedAudio.playSound(\"files/sounds/hit.wav\")", line(BlockDefinition.PlaySound("files/sounds/hit.wav")))
        assertEquals("generatedAudio.playSound(\"files/sounds/hit.wav\", volume = 0.25f)", line(BlockDefinition.PlaySound("files/sounds/hit.wav", NumberValue.Fixed(25f))))
        assertEquals(
            "generatedAudio.playMusic(\"files/music/theme.mp3\", isLooping = false, volume = loudness / 100f)",
            line(BlockDefinition.PlayMusic("files/music/theme.mp3", isLooping = false, volume = NumberValue.Property("loudness"))),
        )
    }
}
