package com.pandulapeter.gameTemplate.gameStressTest.gameObjects.traits

import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
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

data class Destroyable(
    var destructionState: Float = 0f,
    private val dynamic: Dynamic? = null,
    private val visible: Visible? = null,
    private val movable: Movable? = null,
) : Trait<Destroyable> {

    private constructor(state: State) : this(
        destructionState = state.destructionState,
    )

    init {
        dynamic?.registerUpdater { deltaTimeMillis ->
            if (destructionState > 0) {
                if (destructionState < 1f) {
                    destructionState += 0.001f * deltaTimeMillis
                } else {
                    destructionState = 1f
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

    override fun getSerializer(): Serializer<Destroyable> = State(
        destroyable = this,
    )

    @Serializable
    private data class State(
        @SerialName("destructionState") val destructionState: Float = 0f,
    ) : Serializer<Destroyable> {
        constructor(destroyable: Destroyable) : this(
            destructionState =destroyable.destructionState,
        )

        override val typeId = "destroyable"

        override fun instantiate() = Destroyable(
            state = this,
        )

        override fun serialize() = Json.encodeToString(this)
    }
}