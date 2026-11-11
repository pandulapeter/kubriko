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

import com.pandulapeter.kubrikoCreator.actors.HexColorSerializer
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateSprite
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public object DrawPixels : WriteCommand<DrawPixels.Input> {
    override val name: String = "draw_pixels"
    override val description: String = "Draws on a frame of a sprite (the whole image, for one not cut into frames), pixel by pixel: " +
        "any of single pixels, and rows of letters, each letter a color its palette names, and . leaving a pixel as it is. " +
        "Coordinates are pixels from the frame's top left. read_sprite shows what is there."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSprite(input.sprite)
        ?: context.validateFrame(input.sprite, input.frame)
        ?: when {
            input.pixels.isEmpty() && input.rows.isEmpty() -> Rejection(CommandProblem.PIXELS_NONE)
            input.palette.any { it.letter.length != 1 || it.letter == LEAVE } -> Rejection(CommandProblem.PIXELS_PALETTE_KEY)
            else -> input.rows.joinToString("").toSet().filter { letter -> letter.toString() != LEAVE && input.palette.none { it.letter == letter.toString() } }.takeIf { it.isNotEmpty() }
                ?.let { Rejection(CommandProblem.PIXELS_LETTER_UNKNOWN, it.joinToString(" ")) }
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied = context.editSpriteImage(input.sprite) { frames ->
        val frame = frames.frames[input.frame]
        val pixels = input.drawnPixels()
        val outside = pixels.firstOrNull { !frame.contains(it.x, it.y) }
        if (outside != null) {
            SpriteEdit.Rejected(Rejection(CommandProblem.PIXELS_OUTSIDE_FRAME, "${outside.x}, ${outside.y}", frame.width.toString(), frame.height.toString()))
        } else {
            SpriteEdit.Edited(frames.withFrame(input.frame, frame.with(pixels)))
        }
    }

    private const val LEAVE = "."

    @Serializable
    public data class Input(
        @Description("The sprite's path from composeResources, like drawable/ship.png.")
        @SerialName("sprite") val sprite: String,
        @Description("The frame, from 0. A sprite not cut into frames has only frame 0.")
        @SerialName("frame") val frame: Int = 0,
        @Description("Single pixels to draw.")
        @SerialName("pixels") val pixels: List<DrawnPixel> = emptyList(),
        @Description("Rows of letters from left and top down, each letter a pixel in the color palette names for it; . leaves a pixel as it is.")
        @SerialName("rows") val rows: List<String> = emptyList(),
        @Description("Where rows start: their first letter's pixel from the frame's left edge.")
        @SerialName("left") val left: Int = 0,
        @Description("Where rows start: their first row's pixel from the frame's top edge.")
        @SerialName("top") val top: Int = 0,
        @Description("The color of each letter rows use.")
        @SerialName("palette") val palette: List<PaletteColor> = emptyList(),
    ) {
        private val colorOf get() = palette.associate { it.letter to it.color }

        /**
         * Every pixel this draws, the single ones first, then the rows' over them.
         */
        internal fun drawnPixels(): List<Pixel> = pixels.map { Pixel(it.x, it.y, it.color.toInt()) } + rows.flatMapIndexed { row, letters ->
            val colorOf = colorOf
            letters.mapIndexedNotNull { column, letter -> colorOf[letter.toString()]?.let { Pixel(left + column, top + row, it.toInt()) } }
        }
    }

    @Serializable
    public data class PaletteColor(
        @Description("One letter of rows; . can't be one.")
        @SerialName("letter") val letter: String,
        @Description("Its color, as #RRGGBB, or #AARRGGBB (#00000000 is see-through).")
        @SerialName("color") @Serializable(with = HexColorSerializer::class) val color: Long,
    )

    @Serializable
    public data class DrawnPixel(
        @Description("Pixels from the frame's left edge.")
        @SerialName("x") val x: Int,
        @Description("Pixels from the frame's top edge.")
        @SerialName("y") val y: Int,
        @Description("Its color, as #RRGGBB, or #AARRGGBB (#00000000 is see-through).")
        @SerialName("color") @Serializable(with = HexColorSerializer::class) val color: Long,
    )
}
