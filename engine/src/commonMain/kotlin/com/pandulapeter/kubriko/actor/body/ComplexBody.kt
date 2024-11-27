package com.pandulapeter.kubriko.actor.body

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

interface ComplexBody : Body {

    val size: SceneSize
    val pivot: SceneOffset
    val scale: Scale
    val rotation: AngleRadians

    fun DrawScope.drawDebugBounds(color: Color, stroke: Stroke)
}