/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.scenes.ActorStateEdits
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_property_missing
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_property_value_invalid
import kubriko.creator.generated.resources.error_command_scene_actor_missing
import kubriko.creator.generated.resources.error_command_scene_actor_not_block_built
import kubriko.creator.generated.resources.error_command_scene_content_invalid
import kubriko.creator.generated.resources.error_command_size_invalid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActorValueCommandsTest {
    private val brick = ActorTypeDefinition(
        name = "Brick",
        properties = listOf(PropertyDefinition.Number("hits"), PropertyDefinition.Text("label"), PropertyDefinition.Toggle("isGolden")),
    )
    private val placed = SceneActorEntry("Brick", SceneDocument.blockBuiltState(10f, 20f, 100f, 40f))
    private val handWritten = SceneActorEntry("Wall", """{"body":{"position":{"x":1.0,"y":2.0}},"tint":3}""")

    @OptIn(EditorMutation::class)
    private fun TestScope.context() = commandContext(brick).apply {
        scenes.write("main", SceneDocument.format(listOf(placed, handWritten)))
    }

    private fun <I> CommandContext.rejection(command: WriteCommand<I>, input: I) = command.validate(this, input)

    private fun CommandContext.state(index: Int) = SceneDocument.parse(checkNotNull(scenes.read("main")))[index].state

    @Test
    fun anActorsValuesChangeAndAreUndoneToItsWholeStateBefore() = runTest {
        val context = context()
        val input = SetActorValues.Input(
            scene = "main",
            actorId = "i0",
            x = 5f,
            width = 60f,
            rotation = 90f,
            scaleVertical = 2f,
            color = 0xFF112233,
            properties = listOf(ActorValueChanges.PropertyValue("hits", number = 3f), ActorValueChanges.PropertyValue("isGolden", toggle = true)),
        )
        val state = context.assertRoundTrip(Invocation(SetActorValues, input)) { context.state(0) }!!
        assertEquals(5f to 20f, SceneDocument.positionOf(state))
        assertEquals(0xFF112233, ActorStateEdits.colorOf(state))
        assertEquals(JsonPrimitive(3f), ActorStateEdits.propertyOf(state, "hits"))
        assertEquals(JsonPrimitive(true), ActorStateEdits.propertyOf(state, "isGolden"))
        assertEquals(
            """{"x":30.0,"y":20.0}""",
            (SceneDocument.stateObject(state)!!["body"] as JsonObject)["pivot"].toString(),
        )
    }

    @Test
    fun aHandWrittenActorsWholeStateIsReplacedAndUndone() = runTest {
        val context = context()
        val replaced = context.assertRoundTrip(Invocation(SetActorState, SetActorState.Input("main", "i1", """{"tint":4}"""))) { context.state(1) }
        assertEquals("""{"tint":4}""", replaced)
        assertEquals(Rejection(Res.string.error_command_scene_content_invalid), context.rejection(SetActorState, SetActorState.Input("main", "i1", "[]")))
        // A hand-written Actor's body can still be changed.
        assertNull(context.rejection(SetActorValues, SetActorValues.Input("main", "i1", x = 3f)))
    }

    @Test
    fun actorValuesAreValidated() = runTest {
        val context = context()
        fun rejection(input: SetActorValues.Input) = context.rejection(SetActorValues, input)
        assertEquals(Rejection(Res.string.error_command_scene_actor_missing, "main", "i2"), rejection(SetActorValues.Input("main", "i2", x = 1f)))
        assertEquals(Rejection(Res.string.error_command_number_invalid), rejection(SetActorValues.Input("main", "i0", rotation = Float.NaN)))
        assertEquals(Rejection(Res.string.error_command_size_invalid), rejection(SetActorValues.Input("main", "i0", height = 0f)))
        assertEquals(Rejection(Res.string.error_command_scene_actor_not_block_built, "Wall"), rejection(SetActorValues.Input("main", "i1", color = 0xFF000000)))
        assertEquals(
            Rejection(Res.string.error_command_actor_property_missing, "Brick", "lives"),
            rejection(SetActorValues.Input("main", "i0", properties = listOf(ActorValueChanges.PropertyValue("lives", number = 1f)))),
        )
        assertEquals(
            Rejection(Res.string.error_command_property_value_invalid, "hits"),
            rejection(SetActorValues.Input("main", "i0", properties = listOf(ActorValueChanges.PropertyValue("hits", text = "3")))),
        )
        assertEquals(
            Rejection(Res.string.error_command_property_value_invalid, "label"),
            rejection(SetActorValues.Input("main", "i0", properties = listOf(ActorValueChanges.PropertyValue("label", text = "a", toggle = true)))),
        )
        assertNull(rejection(SetActorValues.Input("main", "i0", properties = listOf(ActorValueChanges.PropertyValue("label", text = "gold")))))
    }

    @Test
    fun typingIntoOneFieldIsOneUndoStep() {
        assertEquals(
            SetActorValues.mergeKey(SetActorValues.Input("main", "i0", x = 1f)),
            SetActorValues.mergeKey(SetActorValues.Input("main", "i0", x = 12f)),
        )
        assertEquals(
            false,
            SetActorValues.mergeKey(SetActorValues.Input("main", "i0", x = 1f)) == SetActorValues.mergeKey(SetActorValues.Input("main", "i1", x = 1f)),
        )
    }
}
