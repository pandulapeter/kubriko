/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

internal class MovingBox(state: State) : MovingBoxBase(state), Visible {

    override fun onAppearanceChanged() = Unit

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
    }

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
        @SerialName("boxColor") val boxColor: SerializableColor = Color.Gray,
        @SerialName("isRotatingClockwise") val isRotatingClockwise: Boolean = listOf(true, false).random(),
    ) : Serializable.State<MovingBox> {

        override fun restore() = MovingBox(this)

        override fun serialize() = Json.encodeToString(this)
    }
}
