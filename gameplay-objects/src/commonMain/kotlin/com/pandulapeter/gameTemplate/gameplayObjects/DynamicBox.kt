package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectCreator
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toRadians
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.sin

class DynamicBox private constructor(
    creator: Creator,
) : Box(
    typeId = "dynamicBox",
    color = creator.color,
    edgeSize = creator.edgeSize,
    position = creator.position,
    rotationDegrees = creator.rotationDegrees,
), Scalable {

    @Serializable
    data class Creator(
        val color: SerializableColor,
        val edgeSize: Float,
        val position: SerializableOffset,
        val rotationDegrees: Float,
        val scaleFactor: Float,
    ) : GameObjectCreator<DynamicBox> {

        override fun instantiate() = DynamicBox(this)
    }

    override fun saveState() = Json.encodeToString(
        Creator(
            color = color,
            edgeSize = bounds.width,
            position = position,
            rotationDegrees = rotationDegrees,
            scaleFactor = scaleFactor,
        )
    )

    override var scaleFactor: Float = creator.scaleFactor
    private var isGrowing = true

    override fun update(deltaTimeMillis: Float) {
        super.update(deltaTimeMillis)
        rotationDegrees += 0.1f * deltaTimeMillis
        while (rotationDegrees > 360f) {
            rotationDegrees -= 360f
        }
        if (scaleFactor >= 1.6f) {
            isGrowing = false
        }
        if (scaleFactor <= 0.5f) {
            isGrowing = true
        }
        if (isGrowing) {
            scaleFactor += 0.001f * deltaTimeMillis
        } else {
            scaleFactor -= 0.001f * deltaTimeMillis
        }
        position += Offset(
            x = cos(rotationDegrees.toRadians()),
            y = -sin(rotationDegrees.toRadians()),
        )
    }
}
