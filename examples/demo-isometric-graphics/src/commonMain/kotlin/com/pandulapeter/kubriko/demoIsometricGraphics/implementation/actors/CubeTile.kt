/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors.isometric.Cube
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors.traits.VisibleInWorld
import com.pandulapeter.kubriko.helpers.extensions.abs
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneUnit
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

internal class CubeTile(
    state: State,
) : VisibleInWorld(), Editable<CubeTile> {

    @set:Exposed("height")
    override var height = state.height

    @set:Exposed("positionZ")
    override var positionZ = state.positionZ
        get() = field + bounceZ

    @set:Exposed("color")
    var color = state.color

    @set:Exposed("shouldDrawShadow")
    var shouldDrawShadow = state.shouldDrawShadow

    @set:Exposed("shouldUseTextures")
    var shouldUseTextures = state.shouldUseTextures

    override val isometricRepresentation by lazy {
        Cube(
            color = color,
            shouldDrawShadow = shouldDrawShadow,
            shouldUseTextures = shouldUseTextures,
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

    override fun onAdded(kubriko: Kubriko) {
        super<VisibleInWorld>.onAdded(kubriko)
    }

    private var timeAccumulator = 0
    private var bounceZ = SceneUnit.Zero

    override fun update(deltaTimeInMilliseconds: Int) {
        if (isometricGraphicsDemoManager.shouldRotate.value && height > SceneUnit.Zero) {
            body.rotation += deltaTimeInMilliseconds.rad / 800
        }
        if (isometricGraphicsDemoManager.shouldBounce.value || bounceZ > SceneUnit.Unit * 4) {
            bounceZ = abs(height * (timeAccumulator / 500f).rad.sin)
            timeAccumulator += deltaTimeInMilliseconds
        } else {
            bounceZ = SceneUnit.Zero
        }
        super.update(deltaTimeInMilliseconds)
    }

    private val stroke = Stroke(
        width = 8f,
    )

    override fun DrawScope.draw() {
        drawRect(
            color = color,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            topLeft = Offset(
                x = stroke.width * 0.5f,
                y = stroke.width * 0.5f,
            ),
            size = Size(
                width = body.size.width.raw - stroke.width,
                height = body.size.height.raw - stroke.width,
            ),
            style = stroke,
        )
    }

    override fun save() = State(
        body = body,
        height = height,
        positionZ = positionZ,
        color = color,
        shouldDrawShadow = shouldDrawShadow,
        shouldUseTextures = shouldUseTextures,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
        @SerialName("height") val height: SerializableSceneUnit = SceneUnit.Zero,
        @SerialName("positionZ") val positionZ: SerializableSceneUnit = SceneUnit.Zero,
        @SerialName("color") val color: SerializableColor = Color.LightGray,
        @SerialName("shouldDrawShadow") val shouldDrawShadow: Boolean = true,
        @SerialName("shouldUseTextures") val shouldUseTextures: Boolean = true,
    ) : Serializable.State<CubeTile> {

        override fun restore() = CubeTile(this)

        override fun serialize() = Json.encodeToString(this)
    }
}