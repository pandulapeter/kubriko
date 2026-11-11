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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Plays the running game with keys: one hold, or several one after another ([Input.steps]), and a screenshot once they are
 * released if asked, all in one call, since every call the agent makes re-reads its whole conversation.
 */
public object PressKeys : ActionCommand<PressKeys.Input> {
    override val name: String = "press_keys"
    override val description: String = "Holds keys in the game start_game started, then releases them, while it keeps running: one hold (keys) or several in a " +
        "row (steps). Returns the console meanwhile and, with screenshotWidth, an image once they are released."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Game

    override suspend fun run(context: CommandContext, input: Input): CommandOutcome {
        val steps = stepsOf(input) ?: return CommandOutcome.Rejected(Rejection(CommandProblem.GAME_KEYS_OR_STEPS))
        rejectionOf(steps)?.let { return CommandOutcome.Rejected(it) }
        val console = steps.flatMap { step ->
            when (val done = context.runner.pressKeys(step.keys.map { it.uppercase() }, step.milliseconds.toLong())) {
                is GameStep.Done -> done.console
                else -> return done.toOutcome()
            }
        }
        val width = input.screenshotWidth ?: return GameStep.Done(console).toOutcome()
        return when (val screen = context.runner.captureScreen()) {
            is GameStep.Done -> GameStep.Done(
                console = console + screen.console,
                png = screen.png?.let { png -> withContext(Dispatchers.Default) { ScreenshotImage.edited(png, width.coerceIn(ScreenshotImage.widths)) } },
            )
            else -> screen
        }.toOutcome()
    }

    private fun stepsOf(input: Input) = when {
        input.steps.isEmpty() -> listOf(Step(input.keys, input.milliseconds))
        input.keys.isEmpty() -> input.steps
        else -> null
    }

    private fun rejectionOf(steps: List<Step>): Rejection? {
        if (steps.size > MAXIMUM_STEPS) return Rejection(CommandProblem.GAME_STEPS_COUNT, MAXIMUM_STEPS.toString())
        steps.forEach { step ->
            when {
                step.keys.size !in 1..MAXIMUM_KEYS -> return Rejection(CommandProblem.GAME_KEYS_COUNT, MAXIMUM_KEYS.toString())
                step.milliseconds !in 1..MAXIMUM_MILLISECONDS -> return Rejection(CommandProblem.GAME_DURATION_INVALID, MAXIMUM_MILLISECONDS.toString())
            }
            step.keys.firstOrNull { !isKeyName(it) }?.let { return Rejection(CommandProblem.GAME_KEY_UNKNOWN, it) }
        }
        if (steps.sumOf { it.milliseconds } > MAXIMUM_TOTAL_MILLISECONDS) {
            return Rejection(CommandProblem.GAME_DURATION_INVALID, MAXIMUM_TOTAL_MILLISECONDS.toString())
        }
        return null
    }

    private const val MAXIMUM_KEYS = 10
    private const val MAXIMUM_MILLISECONDS = 10_000
    private const val MAXIMUM_STEPS = 20
    private const val MAXIMUM_TOTAL_MILLISECONDS = 30_000

    @Serializable
    public data class Input(
        @Description("Keys held together, named as in Java's KeyEvent without VK_: LEFT, RIGHT, UP, DOWN, SPACE, ENTER, ESCAPE, SHIFT, CONTROL, A to Z, 0 to 9.")
        @SerialName("keys") val keys: List<String> = emptyList(),
        @Description("Milliseconds to hold them, at most 10000; a tap is about 100.")
        @SerialName("milliseconds") val milliseconds: Int = 150,
        @Description("Holds one after another, in place of keys: at most 20, 30000 milliseconds together.")
        @SerialName("steps") val steps: List<Step> = emptyList(),
        @Description("An image of the game once released, this many pixels wide at most (160 to 1280).")
        @SerialName("screenshotWidth") val screenshotWidth: Int? = null,
    )

    @Serializable
    public data class Step(
        @Description("Keys held together.")
        @SerialName("keys") val keys: List<String>,
        @Description("Milliseconds to hold them.")
        @SerialName("milliseconds") val milliseconds: Int = 150,
    )
}
