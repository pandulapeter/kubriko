/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.abs
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.toOffset
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableRectangleBody
import com.pandulapeter.kubriko.sprites.SpriteManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_slingshot_background
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_slingshot_foreground

internal class Slingshot private constructor(state: State) : Visible, Editable<Slingshot>, Dynamic, PointerInputAware {
    override val body = state.body
    private lateinit var actorManager: ActorManager
    private lateinit var pointerInputManager: PointerInputManager
    private lateinit var spriteManager: SpriteManager
    private lateinit var viewportManager: ViewportManager
    override val drawingOrder = 1f
    override val isAlwaysActive = true

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        pointerInputManager = kubriko.get()
        spriteManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun DrawScope.draw() {
        spriteManager.get(Res.drawable.sprite_slingshot_background)?.let { background ->
            spriteManager.get(Res.drawable.sprite_slingshot_foreground)?.let { foreground ->
                drawImage(background)
                // TODO: Should be separate Actors probably
                drawImage(foreground)
            }
        }
    }

    override fun onPointerDrag(screenOffset: Offset) {
        viewportManager.addToCameraPosition(screenOffset)
    }

    override fun onPointerZoom(position: Offset, factor: Float) {
        viewportManager.multiplyScaleFactor(factor)
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (!pointerInputManager.isPointerPressed.value) {
            val cameraPosition = viewportManager.cameraPosition.value
            if (abs(cameraPosition.x - body.position.x).raw > 0 || abs(cameraPosition.y - body.position.y).raw > 0) {
                viewportManager.addToCameraPosition(
                    -((body.position - cameraPosition) / viewportManager.scaleFactor.value).toOffset(viewportManager) * 0.025f
                )
            }
        }
    }

    override fun save() = State(
        body = body,
    )

    @kotlinx.serialization.Serializable
    internal data class State(
        @SerialName("body") val body: SerializableRectangleBody = RectangleBody(),
    ) : Serializable.State<Slingshot> {

        override fun restore() = Slingshot(this)

        override fun serialize() = Json.encodeToString(this)
    }
}