package com.pandulapeter.gameTemplate.engine.implementation.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.gameTemplate.engine.managers.ViewportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min

internal class ViewportManagerImpl : ViewportManager {

    private val _size = MutableStateFlow(Size.Zero)
    override val size = _size.asStateFlow()
    private val _offset = MutableStateFlow(Offset.Zero)
    override val offset = _offset.asStateFlow()
    private val _scaleFactor = MutableStateFlow(1f)
    override val scaleFactor = _scaleFactor.asStateFlow()

    override fun addToOffset(
        offset: Offset,
    ) = _offset.update { currentValue -> currentValue - (offset / _scaleFactor.value) }

    override fun multiplyScaleFactor(
        scaleFactor: Float
    ) = _scaleFactor.update { currentValue -> max(SCALE_MIN, min(currentValue * scaleFactor, SCALE_MAX)) }

    fun updateSize(size: Size) = _size.update { size }

    companion object {
        private const val SCALE_MIN = 0.2f
        private const val SCALE_MAX = 5f
    }
}