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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateSprite
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.CanvasAnchor
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelOperations
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public object EditSprite : WriteCommand<EditSprite.Input> {
    override val name: String = "edit_sprite"
    override val description: String = "Changes a whole frame of a sprite, or each of its frames: flips, turns by 90 degrees, shifts with wrapping, " +
        "resizes the canvas, or scales by a whole number. Changing the size of the frames of a sprite cut into frames changes them all."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSprite(input.sprite)
        ?: context.validateFrame(input.sprite, input.frame)
        ?: validateOperation(input.operation)

    /**
     * Why [operation] can't be applied to any sprite, whatever its size: a canvas or a scale out of bounds.
     */
    public fun validateOperation(operation: Operation): Rejection? = when (operation) {
        is Operation.ResizeCanvas -> Rejection(SpriteProblem.SPRITE_SIZE_INVALID, MAXIMUM_SIZE.toString()).takeIf { operation.width !in 1..MAXIMUM_SIZE || operation.height !in 1..MAXIMUM_SIZE }
        is Operation.Scale -> Rejection(SpriteProblem.SPRITE_SCALE_INVALID, MAXIMUM_SCALE.toString()).takeIf { operation.factor !in 1..MAXIMUM_SCALE }
        is Operation.Flip, is Operation.Rotate, is Operation.Shift -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied = context.editSpriteImage(input.sprite) { frames ->
        frameMissingFrom(frames, input.sprite, input.frame)?.let { return@editSpriteImage SpriteEdit.Rejected(it) }
        val (frameWidth, frameHeight) = input.operation.sizeOf(frames.frameWidth, frames.frameHeight)
        val columns = frames.sheet?.let { minOf(it.framesPerRow, it.frameCount) } ?: 1
        val rows = frames.sheet?.rowCount ?: 1
        // Checked before any pixel is touched, so a slip of a scale can't build an image too large to hold.
        if (frameWidth.toLong() * columns > MAXIMUM_SIZE || frameHeight.toLong() * rows > MAXIMUM_SIZE) {
            return@editSpriteImage SpriteEdit.Rejected(Rejection(SpriteProblem.SPRITE_SIZE_INVALID, MAXIMUM_SIZE.toString()))
        }
        frames.mapped(input.frame, origin = input.operation::originOf, operation = input.operation::applyTo)?.let(SpriteEdit::Edited)
            ?: SpriteEdit.Rejected(Rejection(SpriteProblem.SPRITE_FRAME_SIZE_ALONE))
    }

    /**
     * How large a sprite's image may grow, so a slip of a scale or a canvas size can't make one too large to load.
     */
    public const val MAXIMUM_SIZE: Int = 4096
    /**
     * How many pixels across one may become at most in one scale.
     */
    public const val MAXIMUM_SCALE: Int = 16

    @Serializable
    public data class Input(
        @Description("The sprite's path from composeResources, like drawable/ship.png.")
        @SerialName("sprite") val sprite: String,
        @Description("What to do.")
        @SerialName("operation") val operation: Operation,
        @Description("The frame, from 0, or left out for each of them.")
        @SerialName("frame") val frame: Int? = null,
    )

    /**
     * What [EditSprite] does to a frame.
     */
    @Serializable
    public sealed interface Operation {
        public fun applyTo(image: PixelImage): PixelImage

        /**
         * The width and height a frame of [width] by [height] pixels has once this is applied to it.
         */
        public fun sizeOf(width: Int, height: Int): Pair<Int, Int> = width to height

        /**
         * Where [sheet]'s origin goes when this is applied to every frame, following the pixels around it.
         */
        public fun originOf(sheet: SpriteSheet): Pair<Int?, Int?> = sheet.originX to sheet.originY

        @Serializable
        @SerialName("flip")
        @Description("Mirrors it, left to right, or top to bottom.")
        public data class Flip(
            @Description("True to swap the top and the bottom, false to swap the left and the right.")
            @SerialName("isVertical") val isVertical: Boolean = false,
        ) : Operation {
            override fun applyTo(image: PixelImage): PixelImage = if (isVertical) PixelOperations.flippedVertically(image) else PixelOperations.flippedHorizontally(image)

            override fun originOf(sheet: SpriteSheet): Pair<Int?, Int?> = if (isVertical) {
                sheet.originX to sheet.originY?.let { sheet.frameHeight - it }
            } else {
                sheet.originX?.let { sheet.frameWidth - it } to sheet.originY
            }
        }

        @Serializable
        @SerialName("rotate")
        @Description("Turns it by 90 degrees, swapping its width and height.")
        public data class Rotate(
            @Description("True to turn it clockwise, false counterclockwise.")
            @SerialName("isClockwise") val isClockwise: Boolean = true,
        ) : Operation {
            override fun applyTo(image: PixelImage): PixelImage = PixelOperations.rotated(image, isClockwise)

            override fun sizeOf(width: Int, height: Int): Pair<Int, Int> = height to width

            override fun originOf(sheet: SpriteSheet): Pair<Int?, Int?> {
                if (sheet.originX == null && sheet.originY == null) return null to null
                val x = sheet.originX ?: (sheet.frameWidth / 2)
                val y = sheet.originY ?: (sheet.frameHeight / 2)
                return if (isClockwise) (sheet.frameHeight - y) to x else y to (sheet.frameWidth - x)
            }
        }

        @Serializable
        @SerialName("shift")
        @Description("Moves its pixels, what leaves one edge coming back at the other.")
        public data class Shift(
            @Description("Pixels to the right, or to the left below 0.")
            @SerialName("x") val x: Int = 0,
            @Description("Pixels down, or up below 0.")
            @SerialName("y") val y: Int = 0,
        ) : Operation {
            override fun applyTo(image: PixelImage): PixelImage = PixelOperations.shifted(image, x, y)
        }

        @Serializable
        @SerialName("resizeCanvas")
        @Description("Gives it a canvas of another size, cutting it where it is smaller and see-through where it is larger.")
        public data class ResizeCanvas(
            @Description("The new width, in pixels.")
            @SerialName("width") val width: Int,
            @Description("The new height, in pixels.")
            @SerialName("height") val height: Int,
            @Description("Where the image keeps its place.")
            @SerialName("anchor") val anchor: CanvasAnchor = CanvasAnchor.CENTER,
        ) : Operation {
            override fun applyTo(image: PixelImage): PixelImage = PixelOperations.resizedCanvas(image, width, height, anchor)

            override fun sizeOf(width: Int, height: Int): Pair<Int, Int> = this.width to this.height

            override fun originOf(sheet: SpriteSheet): Pair<Int?, Int?> =
                sheet.originX?.let { it + anchor.offsetAcross(width - sheet.frameWidth) } to sheet.originY?.let { it + anchor.offsetDown(height - sheet.frameHeight) }
        }

        @Serializable
        @SerialName("scale")
        @Description("Makes each pixel a square of factor by factor pixels.")
        public data class Scale(
            @Description("A whole number from 1 to $MAXIMUM_SCALE.")
            @SerialName("factor") val factor: Int,
        ) : Operation {
            override fun applyTo(image: PixelImage): PixelImage = PixelOperations.scaled(image, factor)

            override fun sizeOf(width: Int, height: Int): Pair<Int, Int> = (width.toLong() * factor).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() to
                (height.toLong() * factor).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

            override fun originOf(sheet: SpriteSheet): Pair<Int?, Int?> = sheet.originX?.let { it * factor } to sheet.originY?.let { it * factor }
        }
    }
}
