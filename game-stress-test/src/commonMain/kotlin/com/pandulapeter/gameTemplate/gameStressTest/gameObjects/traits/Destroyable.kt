package com.pandulapeter.gameTemplate.gameStressTest.gameObjects.traits

import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Movable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.angleTowards
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Destroyable(
    @SerialName("destructionState") var destructionState: Float = 0f,
    @Transient private val dynamic: Dynamic? = null,
    @Transient private val visible: Visible? = null,
    @Transient private val movable: Movable? = null,
) : Trait<Destroyable> {
    override val typeId = "destroyable"

    init {
        dynamic?.registerUpdater { deltaTimeMillis ->
            if (destructionState > 0) {
                if (destructionState < 1f) {
                    destructionState += 0.001f * deltaTimeMillis
                } else {
                    destructionState = 1f
                }
            }
            movable?.run {
                if (speed > 0) {
                    speed -= 0.015f * deltaTimeMillis
                } else {
                    speed = 0f
                }
            }
        }
    }

    fun destroy(characterPosition: Visible) {
        destructionState = 0.01f
        movable?.run {
            directionDegrees = 180f - (visible?.angleTowards(characterPosition) ?: 0f)
            speed = 3f
        }
    }

    override fun serialize() = Json.encodeToString(this)

    override fun deserialize(json: String) = Json.decodeFromString<Destroyable>(json)
}