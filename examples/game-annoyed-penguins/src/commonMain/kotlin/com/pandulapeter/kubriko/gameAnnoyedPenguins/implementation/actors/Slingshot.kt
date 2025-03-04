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

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableRectangleBody
import com.pandulapeter.kubriko.sprites.SpriteManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_slingshot_background
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_slingshot_foreground

internal class Slingshot private constructor(state: State) : Visible, Editable<Slingshot> {
    override val body = state.body
    private lateinit var spriteManager: SpriteManager
    override val drawingOrder = 1f

    override fun onAdded(kubriko: Kubriko) {
        spriteManager = kubriko.get()
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