package com.pandulapeter.kubriko.implementation.manager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min

internal class ViewportManagerImpl : ViewportManager() {

    private val _size = MutableStateFlow(Size.Zero)
    override val size = _size.asStateFlow()
    private val _center = MutableStateFlow(SceneOffset.Zero)
    override val cameraPosition = _center.asStateFlow()
    private val _scaleFactor = MutableStateFlow(1f)
    override val scaleFactor = _scaleFactor.asStateFlow()

    override fun addToCameraPosition(
        offset: Offset,
    ) = _center.update { currentValue -> currentValue - SceneOffset(offset / _scaleFactor.value) }

    override fun setCameraPosition(
        position: SceneOffset,
    ) = _center.update { position }

    override fun multiplyScaleFactor(
        scaleFactor: Float
    ) = _scaleFactor.update { currentValue -> max(SCALE_MIN, min(currentValue * scaleFactor, SCALE_MAX)) }

    fun updateSize(size: Size) = _size.update { size }

    companion object {
        private const val SCALE_MIN = 0.2f
        private const val SCALE_MAX = 5f
    }
}