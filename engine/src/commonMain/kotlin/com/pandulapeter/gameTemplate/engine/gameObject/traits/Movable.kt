package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toRadians
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.sin

@Serializable
data class Movable(
    @SerialName("speed") var speed: Float = 0f,
    @SerialName("directionDegrees") var directionDegrees: Float = 0f,
    @Transient private var dynamic: Dynamic? = null,
    @Transient private var visible: Visible? = null,
) : Trait<Movable> {

    init {
        dynamic?.registerUpdater { deltaTimeInMillis ->
            visible?.run {
                if (speed > 0) {
                    directionDegrees.toRadians().let { directionRadians ->
                        position += Offset(
                            x = cos(directionRadians),
                            y = -sin(directionRadians)
                        ) * speed * deltaTimeInMillis
                    }
                }
            }
        }
    }

    override val typeId = "movable"

    override fun deserialize(json: String) = Json.decodeFromString<Movable>(json)

    override fun serialize() = Json.encodeToString(this)
}