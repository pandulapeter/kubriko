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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateSprite
import com.pandulapeter.kubrikoCreator.foundation.ColorHex
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelOperations
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

public object ReadSprite : ReadCommand<ReadSprite.Input> {
    override val name: String = "read_sprite"
    override val description: String = "Shows a frame of a sprite, or its whole image, scaled up with sharp pixels, with its size, how it is cut into " +
        "frames, the colors it uses, and the project's palette (set_palette). With asRows, also every pixel as rows of letters and their palette, as draw_pixels takes them."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        (context.validateSprite(input.sprite) ?: context.validateFrame(input.sprite, input.frame))?.let { return CommandOutcome.Rejected(it) }
        val frames = context.readSpriteFrames(input.sprite) ?: return CommandOutcome.Rejected(Rejection(SpriteProblem.SPRITE_UNREADABLE, input.sprite))
        val image = input.frame?.let(frames.frames::get) ?: frames.toImage()
        val colors = image.colors()
        if (input.asRows && (image.width > MAXIMUM_ROWS_SIZE || image.height > MAXIMUM_ROWS_SIZE || colors.size >= LETTERS.length)) {
            return CommandOutcome.Rejected(Rejection(SpriteProblem.SPRITE_TOO_LARGE_FOR_ROWS, MAXIMUM_ROWS_SIZE.toString(), (LETTERS.length - 1).toString()))
        }
        val result = buildJsonObject {
            put("width", image.width)
            put("height", image.height)
            frames.sheet?.let { put("sheet", Json.encodeToJsonElement(SpriteSheet.serializer(), it)) }
            context.palette.colors.value.takeIf { it.isNotEmpty() }?.let { palette ->
                putJsonArray("projectPalette") { palette.forEach { add(JsonPrimitive(ColorHex.format(it))) } }
            }
            putJsonArray("colors") { colors.take(MAXIMUM_COLORS).forEach { add(JsonPrimitive(ColorHex.format(it.toLong() and 0xFFFFFFFFL))) } }
            if (input.asRows) putRows(image, colors)
        }
        val scale = (PREVIEW_SIZE / maxOf(image.width, image.height)).coerceIn(1, MAXIMUM_PREVIEW_SCALE)
        return CommandOutcome.Success(result, image = PixelOperations.scaled(image, scale).toPng())
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putRows(image: PixelImage, colors: List<Int>) {
        val letterOf = colors.withIndex().associate { (index, color) -> color to LETTERS[index + 1] }
        putJsonArray("palette") {
            letterOf.forEach { (color, letter) ->
                add(buildJsonObject {
                    put("letter", letter.toString())
                    put("color", ColorHex.format(color.toLong() and 0xFFFFFFFFL))
                })
            }
        }
        putJsonArray("rows") {
            repeat(image.height) { y -> add(JsonPrimitive(String(CharArray(image.width) { x -> letterOf[image[x, y]] ?: LETTERS[0] }))) }
        }
    }

    private const val PREVIEW_SIZE = 512
    private const val MAXIMUM_PREVIEW_SCALE = 16
    private const val MAXIMUM_COLORS = 32
    private const val MAXIMUM_ROWS_SIZE = 128

    /**
     * The letters rows use: `.` for every see-through pixel, the rest one per color.
     */
    private const val LETTERS = ".abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    @Serializable
    public data class Input(
        @Description("The sprite's path from composeResources, like drawable/ship.png.")
        @SerialName("sprite") val sprite: String,
        @Description("The frame, from 0, or left out for the whole image.")
        @SerialName("frame") val frame: Int? = null,
        @Description("Whether to return every pixel as rows of letters too (at most 128 by 128 pixels): exact, but costly for a large image.")
        @SerialName("asRows") val asRows: Boolean = false,
    )
}
