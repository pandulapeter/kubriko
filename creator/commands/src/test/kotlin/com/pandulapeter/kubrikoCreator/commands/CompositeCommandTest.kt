/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetAppearance
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class CompositeCommandTest {
    private val ball = ActorTypeDefinition(name = "Ball")

    private fun composite(vararg parts: Invocation<*>) = Invocation(CompositeCommand, CompositeCommand.Input(parts.toList()))

    private fun width(width: Float) = Invocation(SetAppearance, SetAppearance.Input("Ball", width = width))

    private fun height(height: Float) = Invocation(SetAppearance, SetAppearance.Input("Ball", height = height))

    private val throwing = object : WriteCommand<NoInput> {
        override val name = "throwing"
        override val description = "Throws."
        override val inputSerializer = NoInput.serializer()

        override fun validate(context: CommandContext, input: NoInput): Rejection? = null

        override suspend fun apply(context: CommandContext, input: NoInput): Applied = throw IOException("Locked.")
    }

    @Test
    fun partsAreAppliedInOrderAndUndoneTogether() = runTest {
        val context = commandContext(ball)
        val changed = context.assertRoundTrip(composite(width(10f), height(20f), width(30f))) { context.blockTypes!!.actorType("Ball") }
        assertEquals(30f, changed!!.width)
        assertEquals(20f, changed.height)
    }

    @Test
    fun aPartIsValidatedAgainstWhatTheOnesBeforeItLeft() = runTest {
        val context = commandContext(ball)
        val composite = composite(
            Invocation(AddProperty, AddProperty.Input("Ball", PropertyDefinition.Number("x"))),
            Invocation(EditProperty, EditProperty.Input("Ball", "x", PropertyDefinition.Number("y"))),
        )
        assertNull(composite.validate(context))
        val properties = context.assertRoundTrip(composite) { context.blockTypes!!["Ball"]!!.properties }
        assertEquals(listOf(PropertyDefinition.Number("y")), properties)
    }

    @Test
    fun aRejectedPartRollsBackTheOnesBeforeIt() = runTest {
        val context = commandContext(ball)
        val applied = composite(width(10f), Invocation(EditProperty, EditProperty.Input("Ball", "x", PropertyDefinition.Number("y")))).apply(context)
        assertEquals(Rejection(LookupProblem.PROPERTY_UNKNOWN, "Ball", "x"), applied.failure)
        assertNull(applied.inverse)
        assertEquals(ball, context.blockTypes!!["Ball"])
    }

    @Test
    fun aThrowingPartRollsBackTheOnesBeforeItAndIsThrownOn() = runTest {
        val context = commandContext(ball)
        assertFailsWith<IOException> { composite(width(10f), height(20f), Invocation(throwing, NoInput)).apply(context) }
        assertEquals(ball, context.blockTypes!!["Ball"])
    }

    @Test
    fun aCompositeIsOneStepNamedByItsFirstPartThatNeverMerges() = runTest {
        val context = commandContext(ball)
        val executor = CommandExecutor(context, testScope())
        assertIs<CommandOutcome.Success>(executor.execute(composite(width(10f), height(20f))))
        assertEquals(1, executor.history.value.undoable.size)
        assertEquals("set_appearance", executor.history.value.nextUndoCommandName)
        assertIs<CommandOutcome.Success>(executor.execute(width(15f)))
        assertEquals(2, executor.history.value.undoable.size)
        executor.step(isUndo = true)
        executor.step(isUndo = true)
        assertEquals(ball, context.blockTypes!!["Ball"])
        executor.step(isUndo = false)
        assertEquals(10f, context.blockTypes.actorType("Ball")!!.width)
    }
}
