package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware

internal class ShipDestination : Positionable, PointerInputAware {

    override val body = PointBody()
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        viewportManager = kubriko.get()
    }

    override fun onPointerOffsetChanged(screenOffset: Offset) {
        body.position = screenOffset.toSceneOffset(viewportManager)
    }
}