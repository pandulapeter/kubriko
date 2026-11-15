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
import com.pandulapeter.kubrikoCreator.actors.FULL_VOLUME
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.AUDIO_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.floatLiteral
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.propertyExpression
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.stringLiteral
import com.pandulapeter.kubrikoCreator.actors.fixedPath
import com.pandulapeter.kubrikoCreator.actors.propertyReference

/**
 * The code of the Blocks playing and stopping audio, through `GeneratedAudio` ([AudioCodeGenerator]).
 */
internal object AudioBlockCode {

    val STOP_MUSIC = BlockCode(listOf("$AUDIO_FIELD.stopMusic()"), needs = setOf(BlockNeed.AUDIO))

    fun playSound(block: BlockDefinition.PlaySound, scope: BlockScope) = pathOf(block.sound.fixedPath, block.sound.propertyReference, scope)?.let { path ->
        BlockCode(listOf("$AUDIO_FIELD.playSound($path${volumeArgument(block.volume, scope)})"), needs = setOf(BlockNeed.AUDIO))
    } ?: BlockCode(listOf("// Choose a sound for this Block."))

    fun playMusic(block: BlockDefinition.PlayMusic, scope: BlockScope) = pathOf(block.music.fixedPath, block.music.propertyReference, scope)?.let { path ->
        BlockCode(
            lines = listOf("$AUDIO_FIELD.playMusic($path${if (block.isLooping) "" else ", isLooping = false"}${volumeArgument(block.volume, scope)})"),
            needs = setOf(BlockNeed.AUDIO),
        )
    } ?: BlockCode(listOf("// Choose music for this Block."))

    /**
     * The path an audio Block plays, as a Kotlin expression: the [fixed] one, or what the Property [property] holds,
     * which may be empty and which `GeneratedAudio` then skips; or null when neither is chosen or the Property doesn't
     * exist.
     */
    private fun pathOf(fixed: String?, property: PropertyReference?, scope: BlockScope) = when {
        fixed != null -> stringLiteral(fixed)
        property != null && scope.hasProperty(property) -> propertyExpression(property, scope)
        else -> null
    }

    /**
     * The volume argument of `GeneratedAudio`'s functions for [volume] (in percent), as a fraction, or nothing at full
     * volume, so that games that use none generate what they did before.
     */
    private fun volumeArgument(volume: NumberValue, scope: BlockScope) = when {
        volume == NumberValue.Fixed(FULL_VOLUME) -> ""
        volume is NumberValue.Fixed -> ", volume = ${floatLiteral(volume.value / FULL_VOLUME)}"
        else -> ", volume = ${NumberValueCode.float(volume, scope)} / ${floatLiteral(FULL_VOLUME)}"
    }
}
