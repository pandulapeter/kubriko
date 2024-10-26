package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

data class Character(
    override var position: Offset,
) : GameObject(), Visible, Dynamic {

    override val size = Size(RADIUS * 2, RADIUS * 2)
    override val pivot = size.center
    override var depth = -position.y - pivot.y

    override fun update(deltaTimeMillis: Float) {
        depth = -position.y - pivot.y - 100f
    }

    override fun draw(scope: DrawScope) = scope.drawCircle(
        color = Color.Green,
        radius = RADIUS,
        center = pivot,
    )

    companion object {
        private const val RADIUS = 50f
    }
}
