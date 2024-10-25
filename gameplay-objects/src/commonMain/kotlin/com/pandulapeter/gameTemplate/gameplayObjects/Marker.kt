package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

data class Marker(
    override val position: Offset,
    private val isOrigin: Boolean,
) : GameObject(), Visible {

    override val size = Size(RADIUS, RADIUS)

    override fun draw(scope: DrawScope) = scope.drawCircle(
        color = if (isOrigin) Color.Red else Color.Black,
        radius = RADIUS,
        center = Offset.Zero
    )

    companion object {
        private const val RADIUS = 3f
    }
}
