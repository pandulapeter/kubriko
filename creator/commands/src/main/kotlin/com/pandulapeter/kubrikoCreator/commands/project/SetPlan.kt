/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.ActionCommand
import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.PlanStep
import com.pandulapeter.kubrikoCreator.commands.PlanStepStatus
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Lays out the conversation's plan, or marks how far it got, as a checklist the assistant panel shows with an Approve
 * button while none of it is started. It is Kubriko Creator's own, since not every agent can report a plan through ACP
 * (Claude Code run through the SDK has no todo tool).
 */
public object SetPlan : ActionCommand<SetPlan.Input> {
    override val name: String = "set_plan"
    override val description: String = "Shows your plan as a checklist the user approves; call it again with each step's status as you work. " +
        "Each call replaces the plan; only Kubriko Creator's own panel takes one."
    override val inputSerializer: KSerializer<Input> = Input.serializer()
    override val leavesEverythingAsItWas: Boolean = true

    override suspend fun run(context: CommandContext, input: Input): CommandOutcome = run(context, input, origin = null)

    override suspend fun run(context: CommandContext, input: Input, origin: AiStepOrigin?): CommandOutcome {
        val steps = input.steps.map { PlanStep(it.text.trim(), it.status) }
        if (steps.size !in 1..MAXIMUM_STEPS || steps.any { it.text.isEmpty() || it.text.length > MAXIMUM_STEP_LENGTH || '\n' in it.text }) {
            return CommandOutcome.Rejected(Rejection(CommandProblem.PLAN_INVALID, MAXIMUM_STEPS.toString(), MAXIMUM_STEP_LENGTH.toString()))
        }
        if (origin == null) return CommandOutcome.Rejected(Rejection(CommandProblem.PLAN_OUTSIDE_PANEL))
        context.plans.set(origin.conversationId, steps)
        return CommandOutcome.Success(JsonObject(emptyMap()))
    }

    private const val MAXIMUM_STEPS = 12
    private const val MAXIMUM_STEP_LENGTH = 200

    @Serializable
    public data class Input(
        @Description("The plan's steps, in order.")
        @SerialName("steps") val steps: List<Step>,
    )

    @Serializable
    @Description("A step.")
    public data class Step(
        @Description("One line, in the game's terms.")
        @SerialName("text") val text: String,
        @Description("How far it got.")
        @SerialName("status") val status: PlanStepStatus = PlanStepStatus.PENDING,
    )
}
