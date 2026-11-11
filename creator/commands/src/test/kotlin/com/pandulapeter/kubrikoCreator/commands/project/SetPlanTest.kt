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

import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.PlanStep
import com.pandulapeter.kubrikoCreator.commands.PlanStepStatus
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SetPlanTest {
    @Test
    fun `each conversation's latest plan is kept with its steps' statuses`() = runTest {
        val context = commandContext()
        val executor = CommandExecutor(context, testScope())
        suspend fun setPlan(steps: String, conversationId: String) =
            executor.call(SetPlan.name, Json.parseToJsonElement("""{"steps": $steps}""") as JsonObject, AiStepOrigin(conversationId, "toolu_1"))

        assertIs<CommandOutcome.Success>(setPlan("""[{"text": " Add lives "}, {"text": "Lose a life"}]""", "first"))
        assertEquals(listOf(PlanStep("Add lives", PlanStepStatus.PENDING), PlanStep("Lose a life", PlanStepStatus.PENDING)), context.plans["first"])
        assertIs<CommandOutcome.Success>(setPlan("""[{"text": "Add lives", "status": "completed"}, {"text": "Lose a life", "status": "in_progress"}]""", "first"))
        assertEquals(listOf(PlanStepStatus.COMPLETED, PlanStepStatus.IN_PROGRESS), context.plans["first"]?.map { it.status })
        assertNull(context.plans["second"])
        context.plans.forget("first")
        assertNull(context.plans["first"])
    }

    @Test
    fun `an outside client's plan is refused, since nobody would see it`() = runTest {
        val context = commandContext()
        assertEquals(CommandOutcome.Rejected(Rejection(CommandProblem.PLAN_OUTSIDE_PANEL)), SetPlan.run(context, SetPlan.Input(listOf(SetPlan.Step("Add lives")))))
        assertNull(context.plans[""])
    }

    @Test
    fun `a plan has one to twelve steps of one short line each`() = runTest {
        val context = commandContext()
        val origin = AiStepOrigin("first")
        val invalid = CommandOutcome.Rejected(Rejection(CommandProblem.PLAN_INVALID, "12", "200"))
        assertEquals(invalid, SetPlan.run(context, SetPlan.Input(emptyList()), origin))
        assertEquals(invalid, SetPlan.run(context, SetPlan.Input(List(13) { SetPlan.Step("Step $it") }), origin))
        assertEquals(invalid, SetPlan.run(context, SetPlan.Input(listOf(SetPlan.Step("  "))), origin))
        assertEquals(invalid, SetPlan.run(context, SetPlan.Input(listOf(SetPlan.Step("x".repeat(201)))), origin))
        assertEquals(invalid, SetPlan.run(context, SetPlan.Input(listOf(SetPlan.Step("Two\nlines"))), origin))
        assertNull(context.plans["first"])
    }
}
