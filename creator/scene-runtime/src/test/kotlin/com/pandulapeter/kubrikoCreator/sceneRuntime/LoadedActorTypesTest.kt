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

import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoadedActorTypesTest {

    @Test
    fun theTypesOfTheClassesLoadedLastCreateNewStatesUntilTheyAreReleased() {
        val types = LoadedActorTypes()
        assertEquals(emptySet(), types.typeIds)
        val loaded = LoadedEntryPoint(entryPoint(), close = {})
        types.update(loaded)
        assertEquals(setOf("Spawner", "Broken"), types.typeIds)
        assertEquals("""{"x":3.0,"y":-4.0}""", types.newState("Spawner", 3f, -4f))
        assertNull(types.newState("Broken", 0f, 0f))
        assertNull(types.newState("Missing", 0f, 0f))
        types.release(LoadedEntryPoint(entryPoint(), close = {}))
        assertEquals(setOf("Spawner", "Broken"), types.typeIds)
        types.release(loaded)
        assertEquals(emptySet(), types.typeIds)
        assertNull(types.newState("Spawner", 0f, 0f))
    }

    @Test
    fun classesWhoseEntryPointThrowsHaveNoTypes() {
        val types = LoadedActorTypes()
        types.update(LoadedEntryPoint(object : CreatorEntryPoint {
            override fun createSerializationManager() = error("The project's code threw.")
        }, close = {}))
        assertEquals(emptySet(), types.typeIds)
    }

    private fun entryPoint() = object : CreatorEntryPoint {
        override fun createSerializationManager() = EditableMetadata.newSerializationManagerInstance(
            EditableMetadata(typeId = "Spawner", deserializeState = { SpawnerState(it) }, instantiate = { SpawnerState("""{"x":${it.x.raw},"y":${it.y.raw}}""") }),
            EditableMetadata<Spawner>(typeId = "Broken", deserializeState = { SpawnerState(it) }, instantiate = { error("The project's code threw.") }),
        )
    }

    private class Spawner : Editable<Spawner> {
        override val body = PointBody(SceneOffset.Zero)

        override fun save() = SpawnerState("{}")
    }

    private class SpawnerState(private val json: String) : Serializable.State<Spawner> {
        override fun restore() = Spawner()

        override fun serialize() = json
    }
}
