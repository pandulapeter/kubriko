package com.pandulapeter.gameTemplate.engine.gameObject.properties

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

interface Visible {
    val size: Size
    val pivot: Offset get() = Offset(size.width / 2f, size.height / 2f)
    val position: Offset

    fun draw(scope: DrawScope) = Unit
}