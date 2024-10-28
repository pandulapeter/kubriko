package com.pandulapeter.gameTemplate.gameplayObjects

import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StaticBox private constructor(
    stateHolder: StateHolder,
) : Box<StaticBox>(
    color = stateHolder.color,
    edgeSize = stateHolder.edgeSize,
    position = stateHolder.position,
    rotationDegrees = stateHolder.rotationDegrees,
) {

    @Serializable
    data class StateHolder(
        val color: SerializableColor,
        val edgeSize: Float,
        val position: SerializableOffset,
        val rotationDegrees: Float,
    ) : State<StaticBox> {

        override fun instantiate() = StaticBox(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getState() = StateHolder(
        color = color,
        edgeSize = bounds.width,
        position = position,
        rotationDegrees = rotationDegrees,
    )
}
