package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneSize

internal class SpaceSquadronGameManager : Manager(), Visible {

    override val body = RectangleBody(
        initialSize = SceneSize(720.sceneUnit, 1280.sceneUnit)
    )

    override fun onInitialize(kubriko: Kubriko) = kubriko.get<ActorManager>().add(this)

    override fun DrawScope.draw() {
        drawRect(
            color = Color.Gray,
            size = body.size.raw,
        )
        drawRect(
            color = Color.Black,
            size = body.size.raw,
            style = Stroke(),
        )
        drawLine(
            color= Color.Black,
            start = Offset(0f,0f),
            end = Offset(body.size.width.raw, body.size.height.raw),
        )
        drawLine(
            color= Color.Black,
            start = Offset(body.size.width.raw,0f),
            end = Offset(0f, body.size.height.raw),
        )
    }
}