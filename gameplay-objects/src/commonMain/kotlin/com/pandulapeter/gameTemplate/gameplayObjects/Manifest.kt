package com.pandulapeter.gameTemplate.gameplayObjects

import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectManifest
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectStateWrapper
import kotlinx.serialization.json.Json

object Manifest : GameObjectManifest {

    override fun deserializeState(wrapper: GameObjectStateWrapper): GameObject.State<*> = when (wrapper.typeId) {
        Character.StateHolder::class.qualifiedName -> Json.decodeFromString<Character.StateHolder>(wrapper.serializedState)
        DynamicBox.StateHolder::class.qualifiedName -> Json.decodeFromString<DynamicBox.StateHolder>(wrapper.serializedState)
        StaticBox.StateHolder::class.qualifiedName -> Json.decodeFromString<StaticBox.StateHolder>(wrapper.serializedState)
        Marker.StateHolder::class.qualifiedName -> Json.decodeFromString<Marker.StateHolder>(wrapper.serializedState)
        else -> throw IllegalArgumentException("Unsupported type")
    }
}