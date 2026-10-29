/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class SceneViewTest {
    private val view = SceneView(
        object : CreatorEntryPoint {
            override fun createSerializationManager() = EditableMetadata.newSerializationManagerInstance(
                EditableMetadata(
                    typeId = TYPE_ID,
                    deserializeState = { BoxState(it) },
                    instantiate = { BoxState(SceneDocument.blockBuiltState(it.x.raw, it.y.raw, SIZE, SIZE)) },
                ),
            )
        },
    )

    @AfterTest
    fun tearDown() = view.dispose()

    @Test
    fun aClickPicksTheActorUnderIt() = runTest {
        view.load(listOf(box("a", 0f, 0f), box("b", 100f, 0f)))
        assertEquals("a", pickAt(0f, 0f))
        assertEquals("b", pickAt(100f, 0f))
        assertNull(pickAt(50f, 0f))
    }

    @Test
    fun centeringOnAnActorMovesTheCameraToItsMiddleAndKeepsTheZoom() = runTest {
        view.load(listOf(box("a", 0f, 0f), box("b", 300f, -40f)))
        view.camera = SceneCamera(SceneOffset.Zero, 2f)
        view.centerOn("b")
        assertEquals(SceneCamera(SceneOffset(300f.sceneUnit, (-40f).sceneUnit), 2f), view.camera)
        view.centerOn("ghost")
        assertEquals(SceneOffset(300f.sceneUnit, (-40f).sceneUnit), view.camera.position)
    }

    @Test
    fun aMovedActorIsPickedWhereItIsNowWithoutBeingCreatedAgain() = runTest {
        view.load(listOf(box("a", 0f, 0f), box("b", 100f, 0f)))
        pickAt(0f, 0f)
        val moved = view.actorOf("a")
        view.load(listOf(box("a", 300f, 300f), box("b", 100f, 0f)))
        assertSame(moved, view.actorOf("a"))
        assertNull(pickAt(0f, 0f))
        assertEquals("a", pickAt(300f, 300f))
        assertEquals("b", pickAt(100f, 0f))
    }

    @Test
    fun addedAndRemovedActorsArePickedOrNot() = runTest {
        view.load(listOf(box("a", 0f, 0f), box("b", 100f, 0f)))
        pickAt(0f, 0f)
        view.load(listOf(box("b", 100f, 0f), box("c", 200f, 0f)))
        assertNull(pickAt(0f, 0f))
        assertEquals("b", pickAt(100f, 0f))
        assertEquals("c", pickAt(200f, 0f))
        assertEquals("c", view.entryOf("c")?.id)
        assertNull(view.actorOf("a"))
    }

    @Test
    fun ofOverlappingActorsTheLaterOneIsPicked() = runTest {
        view.load(listOf(box("a", 0f, 0f), box("b", 5f, 5f)))
        assertEquals("b", pickAt(3f, 3f))
        view.load(listOf(box("b", 5f, 5f), box("a", 0f, 0f)))
        assertEquals("a", pickAt(3f, 3f))
    }

    @Test
    fun actorsOfPreviewedTypesAreDrawnFromTheirLookUntilThePreviewEnds() = runTest {
        val look = ActorTypeLook(ActorShape.CIRCLE, color = 0xFF112233, width = 50f, height = 50f)
        view.load(listOf(box("a", 0f, 0f), box("b", 100f, 0f)))
        val real = view.actorOf("b")
        view.load(view.entries, previews = mapOf(TYPE_ID to look))
        val preview = assertIs<PreviewActor>(view.actorOf("a"))
        assertEquals(SceneSize(SIZE.sceneUnit, SIZE.sceneUnit), preview.body.size)
        assertEquals("b", pickAt(100f, 0f), "previews are picked like the Actors they stand for")
        view.load(listOf(box("a", 0f, 0f), box("b", 300f, 0f)))
        assertIs<PreviewActor>(view.actorOf("b"), "a later load keeps the previews")
        view.load(view.entries, previews = mapOf(TYPE_ID to look.copy(shape = ActorShape.RECTANGLE)))
        assertNotSame(preview, view.actorOf("a"), "a changed look creates the previews again")
        view.load(view.entries, previews = emptyMap())
        assertIs<Box>(view.actorOf("a"))
        assertNotSame(real, view.actorOf("b"))
    }

    @Test
    fun anActorOfATypeWithoutClassesYetIsDrawnFromItsLook() = runTest {
        val look = ActorTypeLook(ActorShape.RECTANGLE, color = 0xFF112233, width = 50f, height = 30f)
        view.load(listOf(SceneActorEntry("New", SceneDocument.blockBuiltState(10f, 20f, 50f, 30f), "n")), previews = mapOf("New" to look))
        assertEquals("n", pickAt(10f, 20f))
    }

    private fun pickAt(x: Float, y: Float) = view.actorIdAt(view.toScreen(SceneOffset(x.sceneUnit, y.sceneUnit)))

    private fun box(id: String, x: Float, y: Float) = SceneActorEntry(TYPE_ID, SceneDocument.blockBuiltState(x, y, SIZE, SIZE), id)

    private class Box(position: SceneOffset, size: Float) : Editable<Box> {
        override val body = BoxBody(initialPosition = position, initialSize = SceneSize(size.sceneUnit, size.sceneUnit))

        override fun save() = BoxState(SceneDocument.blockBuiltState(body.position.x.raw, body.position.y.raw, body.size.width.raw, body.size.height.raw))
    }

    private class BoxState(private val json: String) : Serializable.State<Box> {
        override fun restore(): Box {
            val body = Json.parseToJsonElement(json).jsonObject.getValue("body").jsonObject
            val position = body.getValue("position").jsonObject
            val size = body["size"]?.jsonObject?.get("width")?.jsonPrimitive?.float ?: SIZE
            return Box(SceneOffset(position.getValue("x").jsonPrimitive.float.sceneUnit, position.getValue("y").jsonPrimitive.float.sceneUnit), size)
        }

        override fun serialize() = json
    }

    private companion object {
        const val TYPE_ID = "Box"
        const val SIZE = 20f
    }
}
