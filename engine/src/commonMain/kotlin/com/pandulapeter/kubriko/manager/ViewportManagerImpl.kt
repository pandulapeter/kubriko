package com.pandulapeter.kubriko.manager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.extensions.div
import com.pandulapeter.kubriko.extensions.toSceneOffset
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min

internal class ViewportManagerImpl(
    val aspectRatioMode: AspectRatioMode,
    initialScaleFactor: Float,
    private val minimumScaleFactor: Float,
    private val maximumScaleFactor: Float,
    val viewportEdgeBuffer: SceneUnit,
) : ViewportManager() {
    private lateinit var actorManager: ActorManagerImpl
    private val _cameraPosition = MutableStateFlow(SceneOffset.Zero)
    override val cameraPosition = _cameraPosition.asStateFlow()
    private val _size = MutableStateFlow(Size.Zero)
    override val size = _size.asStateFlow()
    val scaleFactorMultiplier = MutableStateFlow(Scale.Unit)
    private val _scaleFactor = MutableStateFlow(Scale(initialScaleFactor, initialScaleFactor))
    override val scaleFactor by autoInitializingLazy {
        combine(_scaleFactor, scaleFactorMultiplier) { scaleFactor, scaleFactorMultiplier ->
            scaleFactor * scaleFactorMultiplier
        }.asStateFlow(Scale.Unit)
    }
    private val _insetPadding = MutableStateFlow(Rect(Offset.Zero, Offset.Zero))
    override val insetPadding = _insetPadding.asStateFlow()
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

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = (kubriko as KubrikoImpl).actorManager
        insetPadding.onEach { insetPadding ->
            actorManager.insetPaddingAwareActors.value.forEach { it.onInsetPaddingChanged(insetPadding) }
        }.launchIn(scope)
    }

    override fun addToCameraPosition(offset: Offset) = _cameraPosition.update { currentValue ->
        currentValue - SceneOffset(offset / scaleFactor.value)
    }

    override fun setCameraPosition(position: SceneOffset) = _cameraPosition.update { position }

    override fun setScaleFactor(scaleFactor: Float) = _scaleFactor.update { currentValue ->
        Scale(
            horizontal = max(minimumScaleFactor, min(scaleFactor, maximumScaleFactor)),
            vertical = max(minimumScaleFactor, min(scaleFactor, maximumScaleFactor)),
        )
    }

    override fun multiplyScaleFactor(scaleFactor: Float) = _scaleFactor.update { currentValue ->
        Scale(
            horizontal = max(minimumScaleFactor, min(currentValue.horizontal * scaleFactor, maximumScaleFactor)),
            vertical = max(minimumScaleFactor, min(currentValue.vertical * scaleFactor, maximumScaleFactor)),
        )
    }

    fun updateSize(size: Size) = _size.update { size }

    fun updateInsetPadding(insetPadding: Rect) = _insetPadding.update { insetPadding }
}