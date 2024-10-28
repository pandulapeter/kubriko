package com.pandulapeter.gameTemplate.gameStressTest.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Marker private constructor(
    state: StateHolder
) : GameObject<Marker>(), Visible {

    override var position: Offset = state.position

    @Serializable
    data class StateHolder(
        @SerialName("position") val position: SerializableOffset = Offset.Zero,
    ) : State<Marker> {

        override val typeId = TYPE_ID

        override fun instantiate() = Marker(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getState() = StateHolder(
        position = position,
    )

    override var bounds = Size(RADIUS * 2, RADIUS * 2)
    override var pivot = bounds.center
    override var depth = -9999999f

    override fun draw(scope: DrawScope) = scope.drawCircle(
        color = if (position == Offset.Zero) Color.Red else Color.Black,
        radius = RADIUS,
        center = bounds.center,
    )

    companion object {
        const val TYPE_ID = "marker"
        private const val RADIUS = 3f
    }
}
