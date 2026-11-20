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

import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.scenes.MoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.PlaceActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorValues
import com.pandulapeter.kubrikoCreator.sceneRuntime.ActorFrame
import com.pandulapeter.kubrikoCreator.sceneRuntime.FrameHandle
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneView
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument

/**
 * The changes the Scene editor makes to the Scene called [sceneName], each as a command through [onCommand], keeping
 * [selection] in step: a placed Actor is selected, and a removed one is no longer.
 */
internal class SceneEditorActions(
    private val sceneName: String,
    private val view: SceneView,
    private val selection: SceneSelection,
    private val onCommand: (Invocation<*>) -> Unit,
) {
    fun move(actorId: String, x: Float, y: Float) = onCommand(Invocation(MoveActor, MoveActor.Input(sceneName, actorId, x, y)))

    fun transform(actorId: String, handle: FrameHandle, after: ActorFrame) = onCommand(Invocation(SetActorValues, transformInputOf(sceneName, actorId, handle, after)))

    fun place(actorTypeName: String, x: Float, y: Float) {
        val actorId = SceneDocument.newId(view.entries.map { it.id })
        selection.select(actorId)
        onCommand(Invocation(PlaceActor, PlaceActor.Input(sceneName, actorTypeName, x, y, actorId)))
    }

    fun remove(actorId: String) {
        selection.select(null)
        onCommand(Invocation(RemoveActor, RemoveActor.Input(sceneName, actorId)))
    }
}
