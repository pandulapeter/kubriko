package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectCreator
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Marker private constructor(
    creator: Creator
) : GameObject("marker"), Visible {

    override var position: Offset = creator.position

    @Serializable
    data class Creator(
        val position: SerializableOffset
    ) : GameObjectCreator<Marker> {

        override fun instantiate() = Marker(this)
    }

    override fun saveState() = Json.encodeToString(
        Creator(
            position = position,
        )
    )

    override var bounds = Size(RADIUS * 2, RADIUS * 2)
    override var pivot = bounds.center
    override var depth = -9999999f

    override fun draw(scope: DrawScope) = scope.drawCircle(
        color = if (position == Offset.Zero) Color.Red else Color.Black,
        radius = RADIUS,
        center = pivot,
    )

    companion object {
        private const val RADIUS = 3f
    }
}
