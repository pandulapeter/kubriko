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
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.scenes.ActorStateEdits
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_id_repeated
import kubriko.creator.generated.resources.error_command_actor_id_taken
import kubriko.creator.generated.resources.error_command_actor_property_missing
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_actors_empty
import kubriko.creator.generated.resources.error_command_index_invalid
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_scene_actor_missing
import kubriko.creator.generated.resources.error_command_scene_content_invalid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BatchActorCommandsTest {
    private val brick = ActorTypeDefinition(name = "Brick", width = 20f, height = 10f, properties = listOf(PropertyDefinition.Number("hits")))
    private val wall = SceneActorEntry("Wall", """{"body":{"position":{"x":1.0,"y":2.0}}}""", "wall")
    private val music = SceneActorEntry("Music", """{"volume":1.0}""", "music")

    @OptIn(EditorMutation::class)
    private fun TestScope.context(vararg actors: SceneActorEntry) = commandContext(brick).apply {
        scenes.write("main", SceneDocument.format(actors.toList()))
    }

    private fun CommandContext.main() = checkNotNull(scenes.read("main")).let(SceneDocument::parse)

    @Test
    fun aGridOfActorsIsPlacedAsOneStep() = runTest {
        val context = context(wall)
        val grid = List(3) { row -> List(4) { column -> PlaceActors.Placement("Brick", column * 20f, row * 10f) } }.flatten()
        val actors = context.assertRoundTrip(Invocation(PlaceActors, PlaceActors.Input("main", grid + PlaceActors.Placement("Brick", 0f, 50f, "last")))) { context.main() }
        assertEquals(14, actors.size)
        assertEquals(listOf("wall"), actors.take(1).map { it.id })
        assertEquals("last", actors.last().id)
        assertEquals(13, actors.drop(1).map { it.id }.toSet().size)
        assertEquals(SceneDocument.blockBuiltState(60f, 20f, 20f, 10f), actors[12].state)

        val executor = CommandExecutor(context, testScope())
        val placed = assertIs<CommandOutcome.Success>(executor.call("place_actors", PlaceActors.Input("main", grid.take(2)).toJson())).result
        assertEquals(context.main().takeLast(2).map { it.id }, placed.jsonObject.getValue("actorIds").jsonArray.map { it.jsonPrimitive.content })
        executor.step(isUndo = true)
        assertEquals(actors, context.main())
    }

    @Test
    fun placingManyIsValidated() = runTest {
        val context = context(wall)
        fun rejection(vararg placements: PlaceActors.Placement) = PlaceActors.validate(context, PlaceActors.Input("main", placements.toList()))
        assertEquals(Rejection(Res.string.error_command_actors_empty), rejection())
        assertEquals(Rejection(Res.string.error_command_actor_type_missing, "Ghost"), rejection(PlaceActors.Placement("Brick", 0f, 0f), PlaceActors.Placement("Ghost", 0f, 0f)))
        assertEquals(Rejection(Res.string.error_command_number_invalid), rejection(PlaceActors.Placement("Brick", Float.NaN, 0f)))
        assertEquals(Rejection(Res.string.error_command_actor_id_taken, "wall"), rejection(PlaceActors.Placement("Brick", 0f, 0f, "wall")))
        assertEquals(Rejection(Res.string.error_command_actor_id_repeated, "a"), rejection(PlaceActors.Placement("Brick", 0f, 0f, "a"), PlaceActors.Placement("Brick", 0f, 0f, "a")))
    }

    @Test
    fun manyActorsAreRemovedAndComeBackInTheirPlaces() = runTest {
        val bricks = List(5) { SceneActorEntry("Brick", SceneDocument.blockBuiltState(it.toFloat(), 0f, 20f, 10f), "b$it") }
        val context = context(bricks[0], wall, bricks[1], music, bricks[2], bricks[3], bricks[4])
        val remaining = context.assertRoundTrip(Invocation(RemoveActors, RemoveActors.Input("main", listOf("b3", "music", "b0", "b4")))) { context.main() }
        assertEquals(listOf("wall", "b1", "b2"), remaining.map { it.id })
        assertEquals(Rejection(Res.string.error_command_scene_actor_missing, "main", "ghost"), RemoveActors.validate(context, RemoveActors.Input("main", listOf("wall", "ghost"))))
        assertEquals(Rejection(Res.string.error_command_actor_id_repeated, "wall"), RemoveActors.validate(context, RemoveActors.Input("main", listOf("wall", "wall"))))
        assertEquals(Rejection(Res.string.error_command_actors_empty), RemoveActors.validate(context, RemoveActors.Input("main", emptyList())))
    }

    @Test
    fun manyActorsAreRestoredOneAfterAnother() = runTest {
        val context = context(wall)
        val input = RestoreActors.Input("main", listOf(RestoreActors.Restored(0, music.typeId, music.state, "music"), RestoreActors.Restored(2, "Wall", "{}", "copy")))
        assertEquals(listOf("music", "wall", "copy"), context.assertRoundTrip(Invocation(RestoreActors, input)) { context.main() }.map { it.id })
        fun rejection(vararg restored: RestoreActors.Restored) = RestoreActors.validate(context, RestoreActors.Input("main", restored.toList()))
        assertEquals(Rejection(Res.string.error_command_index_invalid, "5"), rejection(RestoreActors.Restored(3, "Wall", "{}"), RestoreActors.Restored(5, "Wall", "{}")))
        assertEquals(Rejection(Res.string.error_command_scene_content_invalid), rejection(RestoreActors.Restored(0, "Wall", "[]")))
        assertEquals(Rejection(Res.string.error_command_actor_id_taken, "wall"), rejection(RestoreActors.Restored(0, "Wall", "{}", "wall")))
    }

    @Test
    fun manyActorsValuesChangeAsOneStep() = runTest {
        val bricks = List(3) { SceneActorEntry("Brick", SceneDocument.blockBuiltState(it * 20f, 0f, 20f, 10f), "b$it") }
        val context = context(*bricks.toTypedArray(), wall)
        val changes = bricks.map { ActorValueChanges(it.id, y = 40f, properties = listOf(ActorValueChanges.PropertyValue("hits", number = 2f))) } + ActorValueChanges("wall", x = 9f)
        val actors = context.assertRoundTrip(Invocation(SetActorsValues, SetActorsValues.Input("main", changes))) { context.main() }
        assertEquals(listOf(0f to 40f, 20f to 40f, 40f to 40f, 9f to 2f), actors.map { SceneDocument.positionOf(it.state) })
        assertEquals(List(3) { JsonPrimitive(2f) }, actors.take(3).map { ActorStateEdits.propertyOf(it.state, "hits") })
        fun rejection(vararg changes: ActorValueChanges) = SetActorsValues.validate(context, SetActorsValues.Input("main", changes.toList()))
        assertEquals(
            Rejection(Res.string.error_command_actor_property_missing, "Brick", "lives"),
            rejection(ActorValueChanges("b0", x = 1f), ActorValueChanges("b1", properties = listOf(ActorValueChanges.PropertyValue("lives", number = 1f)))),
        )
        assertEquals(Rejection(Res.string.error_command_actor_id_repeated, "b0"), rejection(ActorValueChanges("b0", x = 1f), ActorValueChanges("b0", y = 1f)))
        assertEquals(Rejection(Res.string.error_command_scene_actor_missing, "main", "ghost"), rejection(ActorValueChanges("ghost", x = 1f)))
    }

    @Test
    fun manyActorsWholeStatesAreReplacedAsOneStep() = runTest {
        val context = context(wall, music)
        val input = SetActorsState.Input("main", listOf(SetActorsState.ActorState("music", """{"volume":0.5}"""), SetActorsState.ActorState("wall", "{}")))
        assertEquals(listOf("{}", """{"volume":0.5}"""), context.assertRoundTrip(Invocation(SetActorsState, input)) { context.main() }.map { it.state })
        assertEquals(
            Rejection(Res.string.error_command_scene_content_invalid),
            SetActorsState.validate(context, SetActorsState.Input("main", listOf(SetActorsState.ActorState("wall", "[]")))),
        )
    }

    private fun PlaceActors.Input.toJson() = Json.encodeToJsonElement(PlaceActors.Input.serializer(), this).jsonObject
}
