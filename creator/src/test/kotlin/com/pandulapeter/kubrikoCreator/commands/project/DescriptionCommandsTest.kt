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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(EditorMutation::class)
class DescriptionCommandsTest {

    @Test
    fun typesAndScenesAreDescribedAndTheChangeCanBeUndone() = runTest {
        val context = commandContext(ActorTypeDefinition("Ball"), ManagerTypeDefinition("Game"))
        context.scenes.write("level", "[]")
        val type = context.assertRoundTrip(Invocation(SetDescription, SetDescription.Input(type = "Ball", description = "the ball"))) { context.blockTypes!!["Ball"]!!.description }
        assertEquals("the ball", type)
        val scene = context.assertRoundTrip(Invocation(SetDescription, SetDescription.Input(scene = "level", description = "the first level"))) { context.scenes.descriptions.of("level") }
        assertEquals("the first level", scene)
        assertEquals(mapOf("level" to "the first level"), context.scenes.descriptions.current.value)
        SetDescription.apply(context, SetDescription.Input(scene = "level", description = " "))
        assertNull(context.scenes.descriptions.of("level"))
    }

    @Test
    fun aDescriptionIsOneShortLineOfSomethingThatExists() = runTest {
        val context = commandContext(ActorTypeDefinition("Ball"))
        listOf(
            SetDescription.Input(description = "nothing"),
            SetDescription.Input(type = "Ball", scene = "level", description = "both"),
            SetDescription.Input(type = "Wall", description = "missing"),
            SetDescription.Input(scene = "level", description = "missing"),
            SetDescription.Input(type = "Ball", description = "two\nlines"),
            SetDescription.Input(type = "Ball", description = "x".repeat(SetDescription.MAXIMUM_LENGTH + 1)),
        ).forEach { input -> assertIs<Rejection>(SetDescription.validate(context, input), input.toString()) }
    }

    @Test
    fun typingADescriptionIsOneUndoStep() = runTest {
        val context = commandContext(ActorTypeDefinition("Ball"))
        val executor = CommandExecutor(context, testScope())
        listOf("t", "th", "the").forEach { executor.execute(Invocation(SetDescription, SetDescription.Input(type = "Ball", description = it))) }
        assertEquals(1, executor.history.value.undoable.size)
        executor.step(isUndo = true)
        assertNull(context.blockTypes!!["Ball"]!!.description)
    }

    @Test
    fun aScenesDescriptionMovesWithItAndComesBackWithIt() = runTest {
        val context = commandContext()
        context.scenes.write("level", "[]")
        context.scenes.descriptions.set("level", "the first level")
        val executor = CommandExecutor(context, testScope())
        executor.call("rename_scene", buildJsonObject { put("scene", "level"); put("newName", "levels/1") })
        assertEquals(mapOf("levels/1" to "the first level"), context.scenes.descriptions.all())
        executor.call("delete_scene", buildJsonObject { put("scene", "levels/1") })
        assertEquals(emptyMap(), context.scenes.descriptions.all())
        executor.step(isUndo = true)
        assertEquals(mapOf("levels/1" to "the first level"), context.scenes.descriptions.all())
    }

    @Test
    fun theDigestIsPlainText() = runTest {
        val context = commandContext(ActorTypeDefinition("Ball", description = "the ball"))
        val result = assertIs<CommandOutcome.Success>(CommandExecutor(context, testScope()).call("get_project_digest", buildJsonObject { })).result
        assertIs<JsonPrimitive>(result)
        assertContains(result.content, "- Ball: the ball.")
    }
}
