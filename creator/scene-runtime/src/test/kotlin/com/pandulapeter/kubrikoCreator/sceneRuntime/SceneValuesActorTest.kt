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
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SceneValuesActorTest {

    @Test
    fun onlyTheClassesGeneratedInsideTheRegistryHoldValuesForManagerTypes() {
        assertTrue(SceneView.isSceneValues(GeneratedActorTypes.Game()))
        assertFalse(SceneView.isSceneValues(Game()))
    }

    private object GeneratedActorTypes {
        class Game : Values<Game>()
    }

    private class Game : Values<Game>()

    private abstract class Values<T : Values<T>> : Editable<T> {
        override val body = PointBody()

        override fun save(): Serializable.State<T> = error("Not saved in this test.")
    }
}
