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
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.ScreenRegion
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public object TakeScreenshot : ActionCommand<TakeScreenshot.Input> {
    override val name: String = "take_screenshot"
    override val description: String = "Returns an image of what the game start_game started shows right now, with what the console showed meanwhile. " +
        "Smaller images cost less: narrow it with width, or crop it to a region or to an Actor type."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Game

    override suspend fun run(context: CommandContext, input: Input): CommandOutcome {
        val located = input.actorType?.let { actorType ->
            when (val step = context.runner.locateActors(actorType)) {
                is GameStep.Done -> step
                else -> return step.toOutcome()
            }
        }
        val region = located?.region?.withMargin(ACTOR_MARGIN, MINIMUM_ACTOR_MARGIN) ?: ScreenRegion(input.left, input.top, input.right, input.bottom)
        return when (val step = context.runner.captureScreen()) {
            is GameStep.Done -> GameStep.Done(
                console = located?.console.orEmpty() + step.console,
                png = step.png?.let { png ->
                    withContext(Dispatchers.Default) {
                        ScreenshotImage.edited(png, input.width.coerceIn(ScreenshotImage.widths), region.left, region.top, region.right, region.bottom)
                    }
                },
            )
            else -> step
        }.toOutcome()
    }

    /**
     * How much room is left around the Actors a screenshot is cropped to, as a fraction of their size and at least of the
     * view, so that what they touch shows too.
     */
    private const val ACTOR_MARGIN = 0.5f
    private const val MINIMUM_ACTOR_MARGIN = 0.05f

    @Serializable
    public data class Input(
        @Description("At most how many pixels wide, from 160 to 1280.")
        @SerialName("width") val width: Int = 1280,
        @Description("The left edge of the region to keep, as a fraction of the width from 0 to 1.")
        @SerialName("left") val left: Float = 0f,
        @Description("The top edge of the region to keep, as a fraction of the height from 0 to 1.")
        @SerialName("top") val top: Float = 0f,
        @Description("The right edge of the region to keep, as a fraction of the width from 0 to 1.")
        @SerialName("right") val right: Float = 1f,
        @Description("The bottom edge of the region to keep, as a fraction of the height from 0 to 1.")
        @SerialName("bottom") val bottom: Float = 1f,
        @Description("Crops to this Actor type's Actors in view, with room around them, in place of the region.")
        @SerialName("actorType") val actorType: String? = null,
    )
}
