package com.pandulapeter.gameTemplate.engine.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.flow.StateFlow

interface ViewportManager {
    val size: StateFlow<Size>
    val offset: StateFlow<Offset>
    val scaleFactor: StateFlow<Float>

    fun addToOffset(offset: Offset)

    fun setOffset(offset: Offset)

    fun multiplyScaleFactor(scaleFactor: Float)
}