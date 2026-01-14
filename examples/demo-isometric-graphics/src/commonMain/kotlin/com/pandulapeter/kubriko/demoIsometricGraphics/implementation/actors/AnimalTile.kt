/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors.isometric.Animal
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors.traits.VisibleInWorld
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

internal class AnimalTile(
    state: State,
) : VisibleInWorld(), Editable<AnimalTile> {

    @set:Exposed("height")
    override var height = state.height

    @set:Exposed("positionZ")
    override var positionZ = state.positionZ

    @set:Exposed("furColor")
    var furColor = state.furColor

    @set:Exposed("eyeColor")
    var eyeColor = state.eyeColor

    override val isometricRepresentation by lazy {
        Animal(
            furColor = furColor,
            eyeColor = eyeColor,
            positionX = body.position.x,
            positionY = body.position.y,
            positionZ = positionZ,
            dimensionX = body.size.width,
            dimensionY = body.size.height,
            dimensionZ = height,
            rotationZ = body.rotation,
        )
    }
    override val body = state.body

    override fun update(deltaTimeInMilliseconds: Int) {
        body.rotation += AngleRadians.TwoPi * isometricGraphicsDemoManager.characterOrientation.value * deltaTimeInMilliseconds / 2000
        if (isometricGraphicsDemoManager.shouldMove.value) {
            val speed = (deltaTimeInMilliseconds * 0.2f).sceneUnit
            body.position = SceneOffset(
                x = body.position.x + speed * body.rotation.cos,
                y = body.position.y + speed * body.rotation.sin,
            )
        }
        isometricRepresentation.updateWithoutProcessing(
            positionX = body.position.x,
            positionY = body.position.y,
            positionZ = positionZ,
            dimensionX = body.size.width,
            dimensionY = body.size.height,
            dimensionZ = height,
            rotationZ = body.rotation,
        )
    }

    private val stroke = Stroke(
        width = 8f,
    )

    override fun DrawScope.draw() {
        val radius = (body.size.width + body.size.height) * 0.25f
        drawCircle(
            color = this@AnimalTile.furColor,
            radius = radius.raw,
            center = body.size.center.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = radius.raw - stroke.width * 0.5f,
            center = body.size.center.raw,
            style = stroke,
        )
    }

    override fun save() = State(
        body = body,
        height = height,
        positionZ = positionZ,
        furColor = furColor,
        eyeColor = eyeColor,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
        @SerialName("height") val height: SerializableSceneUnit = SceneUnit.Zero,
        @SerialName("positionZ") val positionZ: SerializableSceneUnit = SceneUnit.Zero,
        @SerialName("furColor") val furColor: SerializableColor = Color.LightGray,
        @SerialName("eyeColor") val eyeColor: SerializableColor = Color.LightGray,
    ) : Serializable.State<AnimalTile> {

        override fun restore() = AnimalTile(this)

        override fun serialize() = Json.encodeToString(this)
    }
}