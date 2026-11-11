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

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateSprite
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.sprites.pixels.SpriteFrames
import com.pandulapeter.kubrikoCreator.sprites.sheetOfSprite
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public object EditSpriteFrames : WriteCommand<EditSpriteFrames.Input> {
    override val name: String = "edit_sprite_frames"
    override val description: String = "Adds a see-through frame to a sprite, or duplicates, deletes, or moves one of its frames."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSprite(input.sprite)
        ?: input.operation.framesNamed(context.spriteFolders.state.value.sheets.sheetOfSprite(input.sprite)?.frameCount ?: 1)
            .firstNotNullOfOrNull { context.validateFrame(input.sprite, it) }

    override suspend fun apply(context: CommandContext, input: Input): Applied = context.editSpriteImage(input.sprite) { frames ->
        edited(frames, input.sprite, input.operation)
    }

    /**
     * [frames] of the sprite at [sprite] with [operation] applied, as this command applies it (the sprite editor applies it
     * to what it holds the same way), or why it can't be.
     */
    public fun edited(frames: SpriteFrames, sprite: String, operation: Operation): SpriteEdit {
        operation.framesNamed(frames.frames.size).firstNotNullOfOrNull { frameMissingFrom(frames, sprite, it) }?.let { return SpriteEdit.Rejected(it) }
        val changed = operation.applyTo(frames.frames, PixelImage(frames.frameWidth, frames.frameHeight)) ?: return SpriteEdit.Rejected(Rejection(SpriteProblem.SPRITE_FRAME_LAST))
        val framesPerRow = framesPerRowOf(frames.sheet, changed.size, frames.frameWidth)
        val rows = (changed.size + framesPerRow - 1) / framesPerRow
        if (frames.frameHeight.toLong() * rows > EditSprite.MAXIMUM_SIZE) {
            return SpriteEdit.Rejected(Rejection(SpriteProblem.SPRITE_SIZE_INVALID, EditSprite.MAXIMUM_SIZE.toString()))
        }
        return SpriteEdit.Edited(frames.withFrames(changed, framesPerRow))
    }

    /**
     * How many of [count] frames of [frameWidth] pixels go in a row: all of them while the frames were in one row (as an
     * animation strip is), as many as before otherwise, and never more than the widest image allows.
     */
    private fun framesPerRowOf(sheet: SpriteSheet?, count: Int, frameWidth: Int): Int {
        val wanted = if (sheet == null || sheet.framesPerRow >= sheet.frameCount) count else sheet.framesPerRow
        return minOf(wanted, (EditSprite.MAXIMUM_SIZE / frameWidth).coerceAtLeast(1))
    }

    @Serializable
    public data class Input(
        @Description("The sprite's path from composeResources, like drawable/ship.png.")
        @SerialName("sprite") val sprite: String,
        @Description("What to do.")
        @SerialName("operation") val operation: Operation,
    )

    /**
     * What [EditSpriteFrames] does to a sprite's frames.
     */
    @Serializable
    public sealed interface Operation {
        /**
         * The frames this leaves of [frames], a new one being [blank]; null when it would leave none.
         */
        public fun applyTo(frames: List<PixelImage>, blank: PixelImage): List<PixelImage>?

        /**
         * The frames, of a sprite of [frameCount] frames, this names, which have to exist.
         */
        public fun framesNamed(frameCount: Int): List<Int>

        /**
         * The frame to show once this is applied: the one it added or moved, or the one where a deleted one was.
         */
        public fun frameShownAfter(frameCount: Int): Int

        @Serializable
        @SerialName("add")
        @Description("Adds a see-through frame.")
        public data class Add(
            @Description("Its place, from 0; left out for last.")
            @SerialName("at") val at: Int? = null,
        ) : Operation {
            override fun applyTo(frames: List<PixelImage>, blank: PixelImage): List<PixelImage> = frames.toMutableList().apply { add(at ?: size, blank) }

            override fun framesNamed(frameCount: Int): List<Int> = listOfNotNull(at?.takeIf { it != frameCount })

            override fun frameShownAfter(frameCount: Int): Int = at ?: (frameCount - 1)
        }

        @Serializable
        @SerialName("duplicate")
        @Description("Puts a copy of a frame right after it.")
        public data class Duplicate(
            @Description("The frame, from 0.")
            @SerialName("frame") val frame: Int,
        ) : Operation {
            override fun applyTo(frames: List<PixelImage>, blank: PixelImage): List<PixelImage> = frames.toMutableList().apply { add(frame + 1, frames[frame]) }

            override fun framesNamed(frameCount: Int): List<Int> = listOf(frame)

            override fun frameShownAfter(frameCount: Int): Int = frame + 1
        }

        @Serializable
        @SerialName("delete")
        @Description("Deletes a frame.")
        public data class Delete(
            @Description("The frame, from 0.")
            @SerialName("frame") val frame: Int,
        ) : Operation {
            override fun applyTo(frames: List<PixelImage>, blank: PixelImage): List<PixelImage>? = frames.takeIf { it.size > 1 }?.filterIndexed { index, _ -> index != frame }

            override fun framesNamed(frameCount: Int): List<Int> = listOf(frame)

            override fun frameShownAfter(frameCount: Int): Int = frame.coerceAtMost(frameCount - 1)
        }

        @Serializable
        @SerialName("move")
        @Description("Moves a frame to another place.")
        public data class Move(
            @Description("The frame, from 0.")
            @SerialName("frame") val frame: Int,
            @Description("The place it takes, from 0.")
            @SerialName("to") val to: Int,
        ) : Operation {
            override fun applyTo(frames: List<PixelImage>, blank: PixelImage): List<PixelImage> = frames.toMutableList().apply { add(to, removeAt(frame)) }

            override fun framesNamed(frameCount: Int): List<Int> = listOf(frame, to)

            override fun frameShownAfter(frameCount: Int): Int = to
        }
    }
}
