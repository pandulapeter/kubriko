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

import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ActorStateEditsTest {

    @Test
    fun aNewSizeKeepsASavedPivotInProportion() {
        val state = """{"body":{"position":{"x":0.0,"y":0.0},"size":{"width":40.0,"height":20.0},"pivot":{"x":10.0,"y":20.0}}}"""
        assertEquals("""{"x":20.0,"y":10.0}""", pivotOf(ActorStateEdits.withChanges(state, ActorStateEdits.Changes(width = 80f, height = 10f))!!))
    }

    @Test
    fun aNewSizePutsThePivotInTheMiddleWhenTheStateSavedNone() {
        assertEquals("""{"x":30.0,"y":2.0}""", pivotOf(ActorStateEdits.withChanges(SceneDocument.blockBuiltState(0f, 0f, 10f, 4f), ActorStateEdits.Changes(width = 60f))!!))
        val withoutSize = """{"body":{"position":{"x":0.0,"y":0.0},"pivot":{"x":1.0,"y":1.0}}}"""
        assertEquals("""{"x":5.0,"y":3.0}""", pivotOf(ActorStateEdits.withChanges(withoutSize, ActorStateEdits.Changes(width = 10f, height = 6f))!!))
    }

    private fun pivotOf(state: String) = (SceneDocument.stateObject(state)!!["body"] as JsonObject)["pivot"].toString()
}
