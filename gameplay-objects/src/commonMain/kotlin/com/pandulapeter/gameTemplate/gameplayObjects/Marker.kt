package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

data class Marker(
    override var position: Offset,
) : GameObject(), Visible {

    override var bounds = Size(RADIUS * 2, RADIUS * 2)
    override var pivot = bounds.center
    override var depth = -9999999f

    override fun draw(scope: DrawScope) = scope.drawCircle(
        color = if (position == Offset.Zero) Color.Red else Color.Black,
        radius = RADIUS,
        center = pivot,
    )

    companion object {
        private const val RADIUS = 3f
    }
}
