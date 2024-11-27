package com.pandulapeter.kubriko.actor.body

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.types.SceneOffset

interface Body {

    val axisAlignedBoundingBox: AxisAlignedBoundingBox
    var position: SceneOffset

    fun DrawScope.drawDebugBounds(color: Color, stroke: Stroke)
}