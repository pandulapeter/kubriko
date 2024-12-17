package com.pandulapeter.kubriko.manager

import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.manager.ViewportManagerImpl
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
abstract class ViewportManager : Manager() {

    abstract val cameraPosition: StateFlow<SceneOffset> // Center of the viewport
    abstract val size: StateFlow<Size>
    abstract val scaleFactor: StateFlow<Scale>
    abstract val insetPadding: StateFlow<Rect>
    abstract val topLeft: StateFlow<SceneOffset>
    abstract val bottomRight: StateFlow<SceneOffset>

    abstract fun addToCameraPosition(offset: Offset)

    abstract fun setCameraPosition(position: SceneOffset)

    abstract fun setScaleFactor(scaleFactor: Float)

    abstract fun multiplyScaleFactor(scaleFactor: Float)

    sealed class AspectRatioMode {

        data object Dynamic : AspectRatioMode()

        data class FitHorizontal(
            val width: SceneUnit,
        ) : AspectRatioMode()

        data class FitVertical(
            val height: SceneUnit,
        ) : AspectRatioMode()

        data class Fixed(
            val ratio: Float,
            val width: SceneUnit,
            val alignment: Alignment = Alignment.Center,
        ) : AspectRatioMode()

        data class Stretched(
            val size: SceneSize,
        ) : AspectRatioMode()
    }

    companion object {
        fun newInstance(
            aspectRatioMode: AspectRatioMode = AspectRatioMode.Dynamic,
            initialScaleFactor: Float = 1f,
            minimumScaleFactor: Float = 0.2f,
            maximumScaleFactor: Float = 5f,
            viewportEdgeBuffer: SceneUnit = 0f.sceneUnit,
        ): ViewportManager = ViewportManagerImpl(
            aspectRatioMode = aspectRatioMode,
            initialScaleFactor = initialScaleFactor,
            minimumScaleFactor = minimumScaleFactor,
            maximumScaleFactor = maximumScaleFactor,
            viewportEdgeBuffer = viewportEdgeBuffer,
        )
    }
}