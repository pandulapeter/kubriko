package com.pandulapeter.gameTemplate.gameplayObjects

import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectCreator
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StaticBox private constructor(
    creator: Creator,
) : Box(
    typeId = "staticBox",
    color = creator.color,
    edgeSize = creator.edgeSize,
    position = creator.position,
    rotationDegrees = creator.rotationDegrees,
) {

    @Serializable
    data class Creator(
        val color: SerializableColor,
        val edgeSize: Float,
        val position: SerializableOffset,
        val rotationDegrees: Float,
    ) : GameObjectCreator<StaticBox> {

        override fun instantiate() = StaticBox(this)
    }

    override fun saveState() = Json.encodeToString(
        Creator(
            color = color,
            edgeSize = bounds.width,
            position = position,
            rotationDegrees = rotationDegrees,
        )
    )
}
