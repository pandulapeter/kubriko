/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation

import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SceneDocumentTest {

    private val addedActors = mutableListOf<Editable<*>>()
    private val removedActors = mutableListOf<Editable<*>>()
    private val document = SceneDocument(
        serialize = { actors -> actors.joinToString(",") { (it as Named).name } },
        deserialize = { scene -> scene.split(',').filter { it.isNotEmpty() }.map(::actorNamed) },
        addActors = addedActors::addAll,
        removeActors = removedActors::addAll,
    )

    @Test
    fun aUniqueActorReplacesTheTrackedOneOfItsClass() {
        document.replaceSceneActors(listOf(PlainActor("a"), UniqueActor("U1")))
        document.addSceneActor(UniqueActor("U2"))
        assertEquals("a,U2", document.serializeScene())
    }

    @Test
    fun undoAfterADeleteRestoresTheActorAndKeepsTheSelection() {
        val first = PlainActor("a")
        val second = PlainActor("b")
        document.replaceSceneActors(listOf(first, second))
        document.recordSnapshot()
        document.removeSceneActor(second)
        assertEquals("a", document.serializeScene())
        var restoredSelection: Editable<*>? = null
        document.undo(selectedActor = first) { restoredSelection = it }
        assertEquals("a,b", document.serializeScene())
        assertEquals("a", (restoredSelection as Named).name)
        assertSame(addedActors[addedActors.size - 2], restoredSelection)
    }

    @Test
    fun undoAndRedoRestoreTheUnsavedChangesFlag() {
        document.replaceSceneActors(listOf(PlainActor("a")))
        document.recordSnapshot()
        document.addSceneActor(PlainActor("b"))
        document.markSceneAsModified()
        assertTrue(document.isSceneModified.value)
        document.undo(selectedActor = null) {}
        assertFalse(document.isSceneModified.value)
        document.redo(selectedActor = null) {}
        assertTrue(document.isSceneModified.value)
    }

    @Test
    fun consecutivePropertyChangesWithOneKeyRecordOneStep() {
        document.replaceSceneActors(listOf(PlainActor("a")))
        document.onBeforePropertyChange("key")
        document.onBeforePropertyChange("key")
        assertTrue(document.canUndo.value)
        document.undo(selectedActor = null) {}
        assertFalse(document.canUndo.value)
    }

    private interface Named {
        val name: String
    }

    private class PlainActor(override val name: String) : Editable<PlainActor>, Named {
        override val body = BoxBody()
        override fun save() = State(this)
    }

    private class UniqueActor(override val name: String) : Editable<UniqueActor>, Named, Unique {
        override val body = BoxBody()
        override fun save() = State(this)
    }

    private class State<T : Serializable<T>>(private val actor: T) : Serializable.State<T> {
        override fun restore() = actor
        override fun serialize() = ""
    }

    private companion object {
        fun actorNamed(name: String): Editable<*> = if (name.startsWith("U")) UniqueActor(name) else PlainActor(name)
    }
}
