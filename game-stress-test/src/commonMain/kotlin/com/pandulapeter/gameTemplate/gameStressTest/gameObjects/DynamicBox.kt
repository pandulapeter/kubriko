package com.pandulapeter.gameTemplate.gameStressTest.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Scalable
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toRadians
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.sin

class DynamicBox private constructor(
    stateHolder: StateHolder,
) : Box<DynamicBox>(
    color = stateHolder.color,
    edgeSize = stateHolder.edgeSize,
    position = stateHolder.position,
    rotationDegrees = stateHolder.rotationDegrees,
), Scalable {

    @Serializable
    data class StateHolder(
        @SerialName("color") val color: SerializableColor = Color.Gray,
        @SerialName("edgeSize") val edgeSize: Float = 100f,
        @SerialName("position") val position: SerializableOffset = Offset.Zero,
        @SerialName("rotationDegrees") val rotationDegrees: Float = 0f,
        @SerialName("scaleFactor") val scaleFactor: Float = 1f,
    ) : State<DynamicBox> {

        override val typeId = TYPE_ID

        override fun instantiate() = DynamicBox(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getState() = StateHolder(
        color = color,
        edgeSize = bounds.width,
        position = position,
        rotationDegrees = rotationDegrees,
        scaleFactor = scaleFactor,
    )

    override var scaleFactor: Float = stateHolder.scaleFactor
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

    companion object {
        const val TYPE_ID = "dynamicBox"
    }
}
