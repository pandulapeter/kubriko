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

import androidx.compose.ui.focus.FocusRequester
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.sceneRuntime.ActorTypeLook
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneView
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneCanvasGesturesTest {
    private val view = SceneView(
        object : CreatorEntryPoint {
            override fun createSerializationManager(): SerializationManager<EditableMetadata<*>, Editable<*>> = EditableMetadata.newSerializationManagerInstance()
        },
    )

    @AfterTest
    fun tearDown() = view.dispose()

    @Test
    fun aHeldArrowKeysMoveGoesWhereItStartedEvenIfTheCanvasShowsAnotherSceneByThen() {
        val ball = SceneActorEntry("Ball", SceneDocument.blockBuiltState(10f, 20f, 30f, 30f), "ball")
        runBlocking { view.load(listOf(ball), mapOf("Ball" to ActorTypeLook(ActorShape.RECTANGLE, color = 0xFF3366CC, width = 30f, height = 30f))) }
        val first = mutableListOf<List<ActorMove>>()
        val second = mutableListOf<List<ActorMove>>()
        var callbacks = callbacks(onMove = { first += it })
        val gestures = SceneCanvasGestures(view, FocusRequester()) { callbacks }
        assertEquals(true, gestures.nudge(1f, 0f))
        assertEquals(true, gestures.nudge(1f, 0f))
        callbacks = callbacks(onMove = { second += it })
        gestures.commitNudge()
        assertEquals(listOf(listOf(ActorMove("ball", 12f, 20f))), first)
        assertEquals(emptyList(), second)
    }

    private fun callbacks(onMove: (List<ActorMove>) -> Unit) = SceneCanvasGestures.Callbacks(
        selectedIds = listOf("ball"),
        canDrag = { true },
        handleMetrics = HandleMetrics(size = 8f, reach = 8f, rotateDistance = 24f, strokeWidth = 1f),
        grid = SceneGrid(16f, isSnappingByDefault = false),
        isSnapping = false,
        onSelectionChange = {},
        onMove = onMove,
        onTransform = { _, _, _ -> },
        onDragged = {},
    )
}
