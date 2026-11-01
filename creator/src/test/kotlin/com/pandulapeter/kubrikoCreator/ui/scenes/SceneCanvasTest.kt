/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SceneCanvasTest {
    private val placed = SceneActorEntry("Ball", SceneDocument.blockBuiltState(1f, 2f, 3f, 4f), "a")

    @Test
    fun onlyAnActorWhoseFileSavesItsPositionCanBeDragged() {
        assertTrue(canDragActor(placed, isSceneReadable = true))
        assertFalse(canDragActor(SceneActorEntry("HandWritten", "{\"speed\":2.0}", "b"), isSceneReadable = true))
        assertFalse(canDragActor(null, isSceneReadable = true))
    }

    @Test
    fun noActorCanBeDraggedInASceneThatCantBeRead() {
        assertFalse(canDragActor(placed, isSceneReadable = false))
    }
}
