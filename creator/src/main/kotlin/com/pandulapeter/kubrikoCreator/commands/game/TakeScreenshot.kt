/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.game

import com.pandulapeter.kubrikoCreator.commands.ActionCommand
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res

internal object TakeScreenshot : ActionCommand<TakeScreenshot.Input> {
    override val name = "take_screenshot"
    override val description = "Returns an image of what the game start_game started shows right now, with what the console showed meanwhile. " +
        "Smaller images cost less: narrow it with width, or crop it to a region."
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Game

    override suspend fun run(context: CommandContext, input: Input) = when (val step = context.runner.captureScreen()) {
        is GameStep.Done -> GameStep.Done(
            console = step.console,
            png = step.png?.let { png ->
                withContext(Dispatchers.Default) {
                    ScreenshotImage.edited(png, input.width.coerceIn(MINIMUM_WIDTH, MAXIMUM_WIDTH), input.left, input.top, input.right, input.bottom)
                }
            },
        )
        else -> step
    }.toOutcome()

    private const val MINIMUM_WIDTH = 160
    private const val MAXIMUM_WIDTH = 1280

    @Serializable
    data class Input(
        @Description("At most how many pixels wide, from 160 to 1280.")
        @SerialName("width") val width: Int = MAXIMUM_WIDTH,
        @Description("The left edge of the region to keep, as a fraction of the width from 0 to 1.")
        @SerialName("left") val left: Float = 0f,
        @Description("The top edge of the region to keep, as a fraction of the height from 0 to 1.")
        @SerialName("top") val top: Float = 0f,
        @Description("The right edge of the region to keep, as a fraction of the width from 0 to 1.")
        @SerialName("right") val right: Float = 1f,
        @Description("The bottom edge of the region to keep, as a fraction of the height from 0 to 1.")
        @SerialName("bottom") val bottom: Float = 1f,
    )
}
