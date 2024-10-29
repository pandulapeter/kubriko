package com.pandulapeter.gameTemplate.gameStressTest.gameObjects.traits

import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.gameObject.editor.VisibleInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Movable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.angleTowards
import com.pandulapeter.gameTemplate.engine.implementation.extensions.getTrait
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@VisibleInEditor(typeId = "destructible")
class Destructible(
    @set:VisibleInEditor(typeId = "destructionState") var destructionState: Float = 0f,
) : Trait<Destructible>() {
    private val movable by lazy { gameObject.getTrait<Movable>() }
    private val visible by lazy { gameObject.getTrait<Visible>() }

    private constructor(state: State) : this(
        destructionState = state.destructionState,
    )

    override fun initialize() {
        gameObject.getTrait<Dynamic>()?.registerUpdater { deltaTimeMillis ->
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

    override fun getSerializer(): Serializer<Destructible> = State(
        destructible = this,
    )

    @Serializable
    private data class State(
        @SerialName("destructionState") val destructionState: Float = 0f,
    ) : Serializer<Destructible> {
        constructor(destructible: Destructible) : this(
            destructionState = destructible.destructionState,
        )

        override val typeId = "destroyable"

        override fun instantiate() = Destructible(
            state = this,
        )

        override fun serialize() = Json.encodeToString(this)
    }
}