/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime
import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Disposable
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun tearDown() {
        view.dispose()
        createdBoxes.clear()
        onBoxCreated = {}
    }

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
    fun zoomedOutAPixelIsMoreOfTheSceneAndActorsArePickedWhereTheyAreDrawn() = runTest {
        view.load(listOf(box("a", 0f, 0f), box("b", 100f, 0f)))
        view.camera = SceneCamera(SceneOffset.Zero, 0.5f)
        val origin = view.toScene(Offset.Zero)
        assertEquals(200f, (view.toScene(Offset(100f, 0f)).x - origin.x).raw, 0.01f)
        assertEquals(50f, view.toScreen(SceneOffset(100f.sceneUnit, 0f.sceneUnit)).x - view.toScreen(SceneOffset.Zero).x, 0.01f)
        assertEquals("b", pickAt(100f, 0f))
        assertNull(pickAt(50f, 0f))
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
    fun aNewOrderCreatesOnlyTheActorsFromWhereItChangedAgain() = runTest {
        view.load(listOf(box("a", 0f, 0f), box("b", 100f, 0f), box("c", 200f, 0f), box("d", 300f, 0f)))
        assertEquals("a", pickAt(0f, 0f))
        val before = listOf("a", "b", "c", "d").associateWith { view.actorOf(it) }
        view.load(listOf(box("a", 0f, 0f), box("c", 200f, 0f), box("b", 100f, 0f), box("d", 300f, 0f)))
        assertSame(before["a"], view.actorOf("a"))
        assertSame(before["c"], view.actorOf("c"))
        listOf("b", "d").forEach { assertNotSame(before[it], view.actorOf(it)) }
        assertEquals("a", pickAt(0f, 0f))
        assertEquals("b", pickAt(100f, 0f))
        assertEquals("c", pickAt(200f, 0f))
        assertEquals("d", pickAt(300f, 0f))
    }

    @Test
    fun aReorderAlsoCreatesAgainAnActorDrawnOutOfOrderSinceItChanged() = runTest {
        view.load(listOf(box("a", 0f, 0f), box("b", 100f, 0f), box("c", 200f, 0f)))
        view.load(listOf(box("a", 50f, 50f).copy(state = SceneDocument.blockBuiltState(0f, 0f, SIZE * 2, SIZE)), box("b", 100f, 0f), box("c", 200f, 0f)))
        val changed = view.actorOf("a")
        val b = view.actorOf("b")
        view.load(listOf(view.entryOf("a")!!, box("c", 200f, 0f), box("b", 100f, 0f)))
        assertSame(changed, view.actorOf("a"))
        assertNotSame(b, view.actorOf("b"))
        assertEquals("c", pickAt(200f, 0f))
    }

    @Test
    fun aChangedActorDrawnOnTopOfAnotherIsPicked() = runTest {
        view.load(listOf(box("a", 0f, 0f), box("b", 5f, 5f)))
        assertEquals("b", pickAt(3f, 3f))
        view.load(listOf(box("a", 0f, 0f).copy(state = SceneDocument.blockBuiltState(0f, 0f, SIZE + 1f, SIZE)), box("b", 5f, 5f)))
        assertEquals("a", pickAt(3f, 3f))
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
    fun aLoadCancelledAfterALookChangedLetsTheNextLoadDrawTheNewLook() = runTest {
        val look = ActorTypeLook(ActorShape.CIRCLE, color = 0xFF112233, width = 50f, height = 50f)
        val changedLook = look.copy(shape = ActorShape.RECTANGLE)
        view.load(listOf(box("a", 0f, 0f)), previews = mapOf(TYPE_ID to look))
        val before = view.actorOf("a")
        val cancelled = launch { view.load(view.entries, previews = mapOf(TYPE_ID to changedLook)) }
        runCurrent()
        cancelled.cancel()
        view.load(view.entries, previews = mapOf(TYPE_ID to changedLook))
        cancelled.join()
        assertNotSame(before, view.actorOf("a"))
    }

    @Test
    fun aLoadCancelledMidwayStopsCreatingAndDisposesWhatItCreated() = runTest {
        view.load(listOf(box("a", 0f, 0f)))
        createdBoxes.clear()
        val isCreating = CountDownLatch(1)
        val mayFinish = CountDownLatch(1)
        onBoxCreated = {
            isCreating.countDown()
            mayFinish.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
        }
        val cancelled = launch { view.load(listOf(box("a", 0f, 0f), box("b", 100f, 0f), box("c", 200f, 0f))) }
        runCurrent()
        try {
            assertTrue(isCreating.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS))
            cancelled.cancel()
        } finally {
            mayFinish.countDown()
        }
        cancelled.join()
        assertEquals(listOf("a"), view.entries.map { it.id })
        val created = synchronized(createdBoxes) { createdBoxes.toList() }
        assertEquals(1, created.size, "nothing is created once the load is cancelled")
        assertTrue(created.single().isDisposed.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun anActorOfATypeWithoutClassesYetIsDrawnFromItsLook() = runTest {
        val look = ActorTypeLook(ActorShape.RECTANGLE, color = 0xFF112233, width = 50f, height = 30f)
        view.load(listOf(SceneActorEntry("New", SceneDocument.blockBuiltState(10f, 20f, 50f, 30f), "n")), previews = mapOf("New" to look))
        assertEquals("n", pickAt(10f, 20f))
    }

    private fun pickAt(x: Float, y: Float) = view.actorIdAt(view.toScreen(SceneOffset(x.sceneUnit, y.sceneUnit)))

    private fun box(id: String, x: Float, y: Float) = SceneActorEntry(TYPE_ID, SceneDocument.blockBuiltState(x, y, SIZE, SIZE), id)

    private class Box(position: SceneOffset, size: Float) : Editable<Box>, Disposable {
        override val body = BoxBody(initialPosition = position, initialSize = SceneSize(size.sceneUnit, size.sceneUnit))
        val isDisposed = CountDownLatch(1)

        init {
            createdBoxes += this
            onBoxCreated()
        }

        override fun dispose() = isDisposed.countDown()

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
        const val TIMEOUT_IN_SECONDS = 5L
        val createdBoxes: MutableList<Box> = Collections.synchronizedList(ArrayList())

        @Volatile
        var onBoxCreated: () -> Unit = {}
    }

    @Test
    fun actorsThatCouldntBeCreatedAreFoundByTheirIds() {
        val entries = listOf(SceneActorEntry("Ball", "{}", "a"), SceneActorEntry("Gone", "{}", "b"), SceneActorEntry("Ball", "{}", "c"))
        assertEquals(setOf("b"), uncreatedIdsOf(entries, listOf("actor", null, "actor")))
        assertEquals(setOf("c"), uncreatedIdsOf(entries, listOf("actor", "actor")))
        assertEquals(emptySet(), uncreatedIdsOf(emptyList(), emptyList()))
    }
}
