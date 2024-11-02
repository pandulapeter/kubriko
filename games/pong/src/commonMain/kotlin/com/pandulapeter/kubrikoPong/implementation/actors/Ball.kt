package com.pandulapeter.kubrikoPong.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

class Ball(
    override val boundingBox: SceneSize = SceneSize(100f.scenePixel, 100f.scenePixel),
    override var position: SceneOffset = SceneOffset.Zero,
    private val boxColor: Color = Color.Gray,
) : Visible {

    override fun draw(scope: DrawScope) = scope.drawCircle(
        color = boxColor,
        radius = (boundingBox.raw.width + boundingBox.raw.height) / 2,
        center = pivotOffset.raw,
    )
}
