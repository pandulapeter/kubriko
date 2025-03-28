/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneUnit
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

class BoxWithCircle private constructor(state: State) : Visible, Dynamic, Editable<BoxWithCircle> {
    override val body = state.body

    @set:Exposed(name = "isRotatingClockwise")
    var isRotatingClockwise = state.isRotatingClockwise

    @set:Exposed(name = "boxColor")
    var boxColor: Color = state.boxColor

    @set:Exposed(name = "circleRadius")
    var circleRadius: SceneUnit = state.circleRadius

    @set:Exposed(name = "circleColor")
    var circleColor: Color = state.circleColor

    override val layerIndex = 0
    override var drawingOrder = 0f

    override fun update(deltaTimeInMilliseconds: Int) {
        body.rotation += 0.001f.rad * deltaTimeInMilliseconds * (if (isRotatingClockwise) 1 else -1)
    }

    override fun DrawScope.draw() {
        drawRect(
            color = boxColor,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(),
        )
        drawCircle(
            color = circleColor,
            radius = circleRadius.raw,
            center = body.size.center.raw,
        )
        drawCircle(
            color = Color.Black,
            radius = circleRadius.raw,
            center = body.size.center.raw,
            style = Stroke(),
        )
    }

    override fun save() = State(
        body = body,
        boxColor = boxColor,
        circleColor = circleColor,
        circleRadius = circleRadius,
        isRotatingClockwise = isRotatingClockwise,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
        @SerialName("circleColor") val circleColor: SerializableColor = Color.White,
        @SerialName("circleRadius") val circleRadius: SerializableSceneUnit = SceneUnit.Zero,
        @SerialName("isRotatingClockwise") val isRotatingClockwise: Boolean = false,
    ) : Serializable.State<BoxWithCircle> {

        override fun restore() = BoxWithCircle(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
