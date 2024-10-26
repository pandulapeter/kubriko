package com.pandulapeter.gameTemplate.engine.gameObject.properties

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

interface Visible {
    val size: Size
    val pivot: Offset
    val position: Offset
    val depth: Float

    fun draw(scope: DrawScope) = Unit
}