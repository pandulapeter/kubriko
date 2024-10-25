package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

data class StaticBox(
    val color: Color,
    val edgeSize: Float,
    override val position: Offset,
    override val rotationDegrees: Float,
) : GameObject(), Visible, Rotatable, Clickable {

    override val size = Size(edgeSize, edgeSize)
    private var isClicked = false

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = if (isClicked) Color.Black else color,
        topLeft = Offset.Zero,
        size = size,
    )

    override fun onClicked() {
        isClicked = !isClicked
    }
}
