package com.pandulapeter.gameTemplate.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectCreator
import com.pandulapeter.gameTemplate.gameplayObjects.DynamicBox
import com.pandulapeter.gameTemplate.gameplayObjects.Marker
import com.pandulapeter.gameTemplate.gameplayObjects.StaticBox

internal object GameObjects {

    val supportedGameObjectTypes = mapOf<Class<out GameObject>, (Offset) -> GameObjectCreator<out GameObject>>(
        StaticBox::class.java to {
            StaticBox.Creator(
                color = Color.Red,
                edgeSize = 100f,
                position = it,
                rotationDegrees = 0f,
            )
        },
        DynamicBox::class.java to {
            DynamicBox.Creator(
                color = Color.Red,
                edgeSize = 100f,
                position = it,
                rotationDegrees = 0f,
                scaleFactor = 1f,
            )
        },
        Marker::class.java to {
            Marker.Creator(
                position = it,
            )
        },
    )
}