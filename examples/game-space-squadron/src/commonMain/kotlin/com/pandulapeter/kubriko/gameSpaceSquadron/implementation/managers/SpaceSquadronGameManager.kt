package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.gameSpaceSquadron.ViewportHeight
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class SpaceSquadronGameManager : Manager(), Visible, Unique {
    private val viewportManager by manager<ViewportManager>()
    override val body = RectangleBody(
        initialSize = SceneSize(
            width = 100.sceneUnit,
            height = 100.sceneUnit,
        ),
        initialPivot = SceneOffset(
            x = 50.sceneUnit,
            y = 100.sceneUnit,
        )
    )

    override fun onInitialize(kubriko: Kubriko) = kubriko.get<ActorManager>().add(this)

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        viewportManager.insetPadding.value.let { insetPadding ->
            val topLeft = insetPadding.topLeft.toSceneOffset(viewportManager)
            val bottomRight = insetPadding.bottomRight.toSceneOffset(viewportManager)
            body.position = SceneOffset(
                x = topLeft.x - bottomRight.x,
                y = ViewportHeight / 2 + topLeft.y - bottomRight.y - 20.sceneUnit,
            )
        }
    }

    override fun DrawScope.draw() = Unit
}