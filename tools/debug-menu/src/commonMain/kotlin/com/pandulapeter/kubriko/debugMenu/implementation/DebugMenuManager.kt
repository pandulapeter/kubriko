package com.pandulapeter.kubriko.debugMenu.implementation

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.minus
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.combine

internal class DebugMenuManager(gameKubriko: Kubriko) : Manager(), Overlay, Unique {

    private val gameActorManager by lazy { gameKubriko.require<ActorManager>() }
    private val gameMetadataManager by lazy { gameKubriko.require<MetadataManager>() }
    private val gameViewportManager by lazy { gameKubriko.require<ViewportManager>() }
    override val overlayDrawingOrder = Float.MIN_VALUE
    val debugMenuMetadata = combine(
        gameMetadataManager.fps,
        gameActorManager.allActors,
        gameActorManager.visibleActorsWithinViewport,
        gameMetadataManager.runtimeInMilliseconds,
    ) { fps, allActors, visibleActorsWithinViewport, runtimeInMilliseconds ->
        DebugMenuMetadata(
            fps = fps,
            totalActorCount = allActors.count(),
            visibleActorWithinViewportCount = visibleActorsWithinViewport.count(),
            playTimeInSeconds = runtimeInMilliseconds / 1000,
        )
    }

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.require<ActorManager>().add(this)
    }

    override fun DrawScope.drawToViewport() = gameViewportManager.cameraPosition.value.let { viewportCenter ->
        withTransform(
            transformBlock = {
                transformViewport(
                    viewportCenter = viewportCenter,
                    shiftedViewportOffset = (gameViewportManager.size.value / 2f) - viewportCenter,
                    viewportScaleFactor = gameViewportManager.scaleFactor.value,
                )
            },
            drawBlock = {
                gameActorManager.visibleActorsWithinViewport.value.forEach { visible ->
                    drawRect(
                        color = Color.Cyan,
                        topLeft = visible.body.axisAlignedBoundingBox.min.raw - visible.body.pivot.raw,
                        size = visible.body.axisAlignedBoundingBox.size.raw,
                        style = Stroke(),
                    )
                }
            },
        )
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