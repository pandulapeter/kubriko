package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

interface Visible {
    var bounds: Size
    var pivot: Offset
    var position: Offset
    var depth: Float

    fun draw(scope: DrawScope)
}