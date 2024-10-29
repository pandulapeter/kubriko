package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.gameObject.editor.VisibleInEditor
import com.pandulapeter.gameTemplate.engine.implementation.extensions.getTrait
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toRadians
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.sin

@VisibleInEditor(typeId = "movable")
class Movable(
    @set:VisibleInEditor(typeId = "speed") var speed: Float = 0f,
    @set:VisibleInEditor(typeId = "friction") var friction: Float = 0f,
    @set:VisibleInEditor(typeId = "directionDegrees") var directionDegrees: Float = 0f,
) : Trait<Movable>() {

    private constructor(state: State) : this(
        speed = state.speed,
        friction = state.friction,
        directionDegrees = state.directionDegrees,
    )

    override fun initialize() {
        gameObject.getTrait<Visible>()?.let { visible ->
            gameObject.getTrait<Dynamic>()?.registerUpdater { deltaTimeInMillis ->
                if (speed != 0f) {
                    speed -= friction * deltaTimeInMillis
                    if (speed < 0.00001f) {
                        speed = 0f
                    }
                    directionDegrees.toRadians().let { directionRadians ->
                        visible.position += Offset(
                            x = cos(directionRadians),
                            y = -sin(directionRadians)
                        ) * speed * deltaTimeInMillis
                    }
                }
            }
        }
    }

    override fun getSerializer(): Serializer<Movable> = State(
        movable = this,
    )

    @Serializable
    private data class State(
        @SerialName("speed") val speed: Float = 0f,
        @SerialName("friction") val friction: Float = 0f,
        @SerialName("directionDegrees") val directionDegrees: Float = 0f,
    ) : Serializer<Movable> {

        constructor(movable: Movable) : this(
            speed = movable.speed,
            friction = movable.friction,
            directionDegrees = movable.directionDegrees,
        )

        override val typeId = "movable"

        override fun instantiate() = Movable(
            state = this,
        )

        override fun serialize() = Json.encodeToString(this)
    }
}