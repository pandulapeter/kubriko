package com.pandulapeter.kubriko.debugMenu.implementation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.implementation.extensions.minus
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.transformForViewport
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

internal class DebugMenuManager(gameKubriko: Kubriko) : Manager(), Overlay, Unique {

    private val gameActorManager by lazy { gameKubriko.require<ActorManager>() }
    private val gameMetadataManager by lazy { gameKubriko.require<MetadataManager>() }
    private val gameViewportManager by lazy { gameKubriko.require<ViewportManager>() }
    private val isDebugOverlayEnabled = MutableStateFlow(false)
    private val debugColor = Color.Cyan
    override val overlayDrawingOrder = Float.MIN_VALUE
    val debugMenuMetadata = combine(
        gameMetadataManager.fps,
        gameActorManager.allActors,
        gameActorManager.visibleActorsWithinViewport,
        gameMetadataManager.runtimeInMilliseconds,
        isDebugOverlayEnabled,
    ) { fps, allActors, visibleActorsWithinViewport, runtimeInMilliseconds, isDebugOverlayEnabled ->
        DebugMenuMetadata(
            fps = fps,
            totalActorCount = allActors.count(),
            visibleActorWithinViewportCount = visibleActorsWithinViewport.count(),
            playTimeInSeconds = runtimeInMilliseconds / 1000,
            isDebugOverlayEnabled = isDebugOverlayEnabled,
        )
    }

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.require<ActorManager>().add(this)
    }

    fun onIsDebugOverlayEnabledChanged() = isDebugOverlayEnabled.update { currentValue -> !currentValue }

    override fun DrawScope.drawToViewport() {
        if (isDebugOverlayEnabled.value) {
            gameViewportManager.cameraPosition.value.let { viewportCenter ->
                gameViewportManager.scaleFactor.value.let { scaleFactor ->
                    withTransform(
                        transformBlock = {
                            transformViewport(
                                viewportCenter = viewportCenter,
                                shiftedViewportOffset = (gameViewportManager.size.value / 2f) - viewportCenter,
                                viewportScaleFactor = scaleFactor,
                            )
                        },
                        drawBlock = {
                            val stroke = Stroke(
                                width = 3f / scaleFactor,
                                join = StrokeJoin.Round,
                            )
                            gameActorManager.visibleActorsWithinViewport.value.forEach { visible ->
                                drawRect(
                                    color = debugColor,
                                    topLeft = visible.body.axisAlignedBoundingBox.min.raw,
                                    size = visible.body.axisAlignedBoundingBox.size.raw,
                                    style = stroke,
                                )
                                withTransform(
                                    transformBlock = { visible.transformForViewport(this) },
                                    drawBlock = { with(visible.body) { drawDebugBounds(debugColor, stroke) } },
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private fun DrawTransform.transformViewport(
        viewportCenter: SceneOffset,
        shiftedViewportOffset: SceneOffset,
        viewportScaleFactor: Float,
    ) {
        translate(
            left = shiftedViewportOffset.x.raw,
            top = shiftedViewportOffset.y.raw,
        )
        scale(
            scaleX = viewportScaleFactor,
            scaleY = viewportScaleFactor,
            pivot = viewportCenter.raw,
        )
    }
}