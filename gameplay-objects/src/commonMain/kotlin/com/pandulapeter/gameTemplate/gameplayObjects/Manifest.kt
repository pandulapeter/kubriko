package com.pandulapeter.gameTemplate.gameplayObjects

import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectManifest
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectStateWrapper
import kotlinx.serialization.json.Json

object Manifest : GameObjectManifest {

    override fun deserializeState(wrapper: GameObjectStateWrapper): GameObject.State<*> = when (wrapper.typeId) {
        "character" -> Json.decodeFromString<Character.StateHolder>(wrapper.serializedState)
        "dynamicBox" -> Json.decodeFromString<DynamicBox.StateHolder>(wrapper.serializedState)
        "staticBox" -> Json.decodeFromString<StaticBox.StateHolder>(wrapper.serializedState)
        "marker" -> Json.decodeFromString<Marker.StateHolder>(wrapper.serializedState)
        else -> throw IllegalArgumentException("Unsupported type")
    }
}