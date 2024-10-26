package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toRadians
import kotlin.math.cos
import kotlin.math.sin

class DynamicBox(
    color: Color,
    edgeSize: Float,
    position: Offset,
    rotationDegrees: Float,
    override var scaleFactor: Float,
) : Box(
    color = color,
    edgeSize = edgeSize,
    position = position,
    rotationDegrees = rotationDegrees,
), Scalable {

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
