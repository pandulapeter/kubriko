package com.pandulapeter.kubriko.implementation.manager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.implementation.extensions.div
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min

internal class ViewportManagerImpl(
    val aspectRatioMode: AspectRatioMode, // TODO: Handle different aspect ratio modes.
    val viewportEdgeBuffer: SceneUnit,
) : ViewportManager() {

    private val _cameraPosition = MutableStateFlow(SceneOffset.Zero)
    override val cameraPosition = _cameraPosition.asStateFlow()
    private val _size = MutableStateFlow(Size.Zero)
    override val size = _size.asStateFlow()

    // TODO: Should be separated to horizontal and vertical scale
    var scaleFactorMultiplier = MutableStateFlow(Scale.Unit)
    private var _scaleFactor = MutableStateFlow(Scale.Unit)
    override val scaleFactor by autoInitializingLazy {
        combine(_scaleFactor, scaleFactorMultiplier) { scaleFactor, scaleFactorMultiplier ->
            scaleFactor * scaleFactorMultiplier
        }.asStateFlow(Scale.Unit)
    }
    override val topLeft by autoInitializingLazy {
        combine(cameraPosition, size, scaleFactor) { viewportCenter, viewportSize, scaleFactor ->
            Offset.Zero.toSceneOffset(
                viewportCenter = viewportCenter,
                viewportSize = viewportSize,
                viewportScaleFactor = scaleFactor,
            )
        }.asStateFlow(SceneOffset.Zero)
    }
    override val bottomRight by autoInitializingLazy {
        combine(cameraPosition, size, scaleFactor) { viewportCenter, viewportSize, scaleFactor ->
            Offset(viewportSize.width, viewportSize.height).toSceneOffset(
                viewportCenter = viewportCenter,
                viewportSize = viewportSize,
                viewportScaleFactor = scaleFactor,
            )
        }.asStateFlow(SceneOffset.Zero)
    }

    override fun addToCameraPosition(offset: Offset) = _cameraPosition.update { currentValue ->
        currentValue - SceneOffset(offset / scaleFactor.value)
    }

    override fun setCameraPosition(position: SceneOffset) = _cameraPosition.update { position }

    override fun multiplyScaleFactor(scaleFactor: Float) = _scaleFactor.update { currentValue ->
        Scale(
            horizontal = max(SCALE_MIN, min(currentValue.horizontal * scaleFactor, SCALE_MAX)),
            vertical = max(SCALE_MIN, min(currentValue.vertical * scaleFactor, SCALE_MAX)),
        )
    }

    fun updateSize(size: Size) = _size.update { size }

    companion object {
        private const val SCALE_MIN = 0.2f
        private const val SCALE_MAX = 5f
    }
}