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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.ActorStateEdits
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        assertEquals(Rejection(CommandProblem.ACTORS_EMPTY), rejection())
        assertEquals(Rejection(CommandProblem.ACTOR_TYPE_MISSING, "Ghost"), rejection(PlaceActors.Placement("Brick", 0f, 0f), PlaceActors.Placement("Ghost", 0f, 0f)))
        assertEquals(Rejection(CommandProblem.NUMBER_INVALID), rejection(PlaceActors.Placement("Brick", Float.NaN, 0f)))
        assertEquals(Rejection(CommandProblem.ACTOR_ID_TAKEN, "wall"), rejection(PlaceActors.Placement("Brick", 0f, 0f, "wall")))
        assertEquals(Rejection(CommandProblem.ACTOR_ID_REPEATED, "a"), rejection(PlaceActors.Placement("Brick", 0f, 0f, "a"), PlaceActors.Placement("Brick", 0f, 0f, "a")))
    }

    @Test
    fun manyActorsAreRemovedAndComeBackInTheirPlaces() = runTest {
        val bricks = List(5) { SceneActorEntry("Brick", SceneDocument.blockBuiltState(it.toFloat(), 0f, 20f, 10f), "b$it") }
        val context = context(bricks[0], wall, bricks[1], music, bricks[2], bricks[3], bricks[4])
        val remaining = context.assertRoundTrip(Invocation(RemoveActors, RemoveActors.Input("main", listOf("b3", "music", "b0", "b4")))) { context.main() }
        assertEquals(listOf("wall", "b1", "b2"), remaining.map { it.id })
        assertEquals(Rejection(CommandProblem.SCENE_ACTOR_MISSING, "main", "ghost"), RemoveActors.validate(context, RemoveActors.Input("main", listOf("wall", "ghost"))))
        assertEquals(Rejection(CommandProblem.ACTOR_ID_REPEATED, "wall"), RemoveActors.validate(context, RemoveActors.Input("main", listOf("wall", "wall"))))
        assertEquals(Rejection(CommandProblem.ACTORS_EMPTY), RemoveActors.validate(context, RemoveActors.Input("main", emptyList())))
    }

    @Test
    fun manyActorsAreRestoredOneAfterAnother() = runTest {
        val context = context(wall)
        val input = RestoreActors.Input("main", listOf(RestoreActors.Restored(0, music.typeId, music.state, "music"), RestoreActors.Restored(2, "Wall", "{}", "copy")))
        assertEquals(listOf("music", "wall", "copy"), context.assertRoundTrip(Invocation(RestoreActors, input)) { context.main() }.map { it.id })
        fun rejection(vararg restored: RestoreActors.Restored) = RestoreActors.validate(context, RestoreActors.Input("main", restored.toList()))
        assertEquals(Rejection(CommandProblem.INDEX_INVALID, "5"), rejection(RestoreActors.Restored(3, "Wall", "{}"), RestoreActors.Restored(5, "Wall", "{}")))
        assertEquals(Rejection(CommandProblem.SCENE_CONTENT_INVALID), rejection(RestoreActors.Restored(0, "Wall", "[]")))
        assertEquals(Rejection(CommandProblem.ACTOR_ID_TAKEN, "wall"), rejection(RestoreActors.Restored(0, "Wall", "{}", "wall")))
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
            Rejection(CommandProblem.ACTOR_PROPERTY_MISSING, "Brick", "lives"),
            rejection(ActorValueChanges("b0", x = 1f), ActorValueChanges("b1", properties = listOf(ActorValueChanges.PropertyValue("lives", number = 1f)))),
        )
        assertEquals(Rejection(CommandProblem.ACTOR_ID_REPEATED, "b0"), rejection(ActorValueChanges("b0", x = 1f), ActorValueChanges("b0", y = 1f)))
        assertEquals(Rejection(CommandProblem.SCENE_ACTOR_MISSING, "main", "ghost"), rejection(ActorValueChanges("ghost", x = 1f)))
    }

    @Test
    fun manyActorsWholeStatesAreReplacedAsOneStep() = runTest {
        val context = context(wall, music)
        val input = SetActorsState.Input("main", listOf(SetActorsState.ActorState("music", """{"volume":0.5}"""), SetActorsState.ActorState("wall", "{}")))
        assertEquals(listOf("{}", """{"volume":0.5}"""), context.assertRoundTrip(Invocation(SetActorsState, input)) { context.main() }.map { it.state })
        assertEquals(
            Rejection(CommandProblem.SCENE_CONTENT_INVALID),
            SetActorsState.validate(context, SetActorsState.Input("main", listOf(SetActorsState.ActorState("wall", "[]")))),
        )
    }

    private fun PlaceActors.Input.toJson() = Json.encodeToJsonElement(PlaceActors.Input.serializer(), this).jsonObject

    @Test
    fun singleAndBatchFormsAgree() = runTest {
        val brickActor = SceneActorEntry("Brick", SceneDocument.blockBuiltState(0f, 0f, 20f, 10f), "brick")
        val (placedOne, placedMany) = assertSameChange(
            Invocation(PlaceActor, PlaceActor.Input("main", "Brick", 5f, 6f, "placed")),
            Invocation(PlaceActors, PlaceActors.Input("main", listOf(PlaceActors.Placement("Brick", 5f, 6f, "placed")))),
            wall,
        )
        assertEquals("placed", placedOne.jsonObject.getValue("actorId").jsonPrimitive.content)
        assertEquals(listOf("placed"), placedMany.jsonObject.getValue("actorIds").jsonArray.map { it.jsonPrimitive.content })
        val hits = listOf(ActorValueChanges.PropertyValue("hits", number = 2f))
        assertSameChange(
            Invocation(SetActorValues, SetActorValues.Input("main", "brick", x = 3f, width = 4f, properties = hits)),
            Invocation(SetActorsValues, SetActorsValues.Input("main", listOf(ActorValueChanges("brick", x = 3f, width = 4f, properties = hits)))),
            wall,
            brickActor,
        )
        assertSameChange(
            Invocation(SetActorState, SetActorState.Input("main", "wall", """{"a":1}""")),
            Invocation(SetActorsState, SetActorsState.Input("main", listOf(SetActorsState.ActorState("wall", """{"a":1}""")))),
            wall,
            music,
        )
        assertSameChange(Invocation(RemoveActor, RemoveActor.Input("main", "music")), Invocation(RemoveActors, RemoveActors.Input("main", listOf("music"))), wall, music)
        val (restoredOne, restoredMany) = assertSameChange(
            Invocation(RestoreActor, RestoreActor.Input("main", 1, "Wall", "{}", "restored")),
            Invocation(RestoreActors, RestoreActors.Input("main", listOf(RestoreActors.Restored(1, "Wall", "{}", "restored")))),
            wall,
            music,
        )
        assertEquals("restored", restoredOne.jsonObject.getValue("actorId").jsonPrimitive.content)
        assertEquals(listOf("restored"), restoredMany.jsonObject.getValue("actorIds").jsonArray.map { it.jsonPrimitive.content })

        fun place(scene: String = "main", type: String = "Brick", x: Float = 0f, id: String? = null) =
            Invocation(PlaceActor, PlaceActor.Input(scene, type, x, 0f, id)) to Invocation(PlaceActors, PlaceActors.Input(scene, listOf(PlaceActors.Placement(type, x, 0f, id))))
        listOf(place(scene = "other"), place(type = "Ghost"), place(x = Float.NaN), place(id = "wall"), place(id = "not an id")).forEach { (single, batch) -> assertSameRejection(single, batch) }
        fun setValues(scene: String = "main", id: String = "brick", x: Float = 0f) =
            Invocation(SetActorValues, SetActorValues.Input(scene, id, x = x)) to Invocation(SetActorsValues, SetActorsValues.Input(scene, listOf(ActorValueChanges(id, x = x))))
        listOf(setValues(scene = "other"), setValues(id = "ghost"), setValues(x = Float.POSITIVE_INFINITY)).forEach { (single, batch) -> assertSameRejection(single, batch, brickActor) }
        fun setState(scene: String = "main", id: String = "wall", state: String = "{}") =
            Invocation(SetActorState, SetActorState.Input(scene, id, state)) to Invocation(SetActorsState, SetActorsState.Input(scene, listOf(SetActorsState.ActorState(id, state))))
        listOf(setState(scene = "other"), setState(id = "ghost"), setState(state = "[1]")).forEach { (single, batch) -> assertSameRejection(single, batch) }
        fun remove(scene: String = "main", id: String = "wall") =
            Invocation(RemoveActor, RemoveActor.Input(scene, id)) to Invocation(RemoveActors, RemoveActors.Input(scene, listOf(id)))
        listOf(remove(scene = "other"), remove(id = "ghost")).forEach { (single, batch) -> assertSameRejection(single, batch) }
        fun restore(scene: String = "main", index: Int = 0, state: String = "{}", id: String? = null) =
            Invocation(RestoreActor, RestoreActor.Input(scene, index, "Wall", state, id)) to
                Invocation(RestoreActors, RestoreActors.Input(scene, listOf(RestoreActors.Restored(index, "Wall", state, id))))
        listOf(restore(scene = "other"), restore(index = 5), restore(state = "nope"), restore(id = "wall")).forEach { (single, batch) -> assertSameRejection(single, batch) }
    }

    /**
     * Applies [single] and [batch] to two copies of a Scene of [actors], then their inverses, checking that both leave the
     * same file each time, and returns their results.
     */
    private suspend fun TestScope.assertSameChange(single: Invocation<*>, batch: Invocation<*>, vararg actors: SceneActorEntry): Pair<JsonElement, JsonElement> {
        val singleContext = context(*actors)
        val batchContext = context(*actors)
        assertNull(single.validate(singleContext))
        assertNull(batch.validate(batchContext))
        val singleApplied = single.apply(singleContext)
        val batchApplied = batch.apply(batchContext)
        assertEquals(batchContext.scenes.read("main"), singleContext.scenes.read("main"), single.command.name)
        assertNotEquals(context(*actors).scenes.read("main"), singleContext.scenes.read("main"), single.command.name)
        assertNotNull(singleApplied.inverse).apply(singleContext)
        assertNotNull(batchApplied.inverse).apply(batchContext)
        assertEquals(batchContext.scenes.read("main"), singleContext.scenes.read("main"), single.command.name)
        return singleApplied.result to batchApplied.result
    }

    private fun TestScope.assertSameRejection(single: Invocation<*>, batch: Invocation<*>, vararg actors: SceneActorEntry) {
        val context = context(wall, music, *actors)
        val rejection = assertNotNull(single.validate(context), "${single.command.name} accepted ${single.input}")
        assertEquals(rejection, batch.validate(context), single.command.name)
    }

    @Test
    fun restoreActorRejectsWhatRestoreActorsRejects() = runTest {
        val context = context(wall)
        listOf("[1]", "nope", "\"text\"", "").forEach { state ->
            val rejection = Rejection(CommandProblem.SCENE_CONTENT_INVALID)
            assertEquals(rejection, RestoreActor.validate(context, RestoreActor.Input("main", 0, "Wall", state)), state)
            assertEquals(rejection, RestoreActors.validate(context, RestoreActors.Input("main", listOf(RestoreActors.Restored(0, "Wall", state)))), state)
        }
        assertNull(RestoreActor.validate(context, RestoreActor.Input("main", 0, "", "{}")))
        assertNull(RestoreActors.validate(context, RestoreActors.Input("main", listOf(RestoreActors.Restored(0, "", "{}")))))
    }
}
