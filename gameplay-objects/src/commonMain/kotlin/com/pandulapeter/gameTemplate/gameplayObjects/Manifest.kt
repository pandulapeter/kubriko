package com.pandulapeter.gameTemplate.gameplayObjects

import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectCreator
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectManifest
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectStateWrapper
import kotlinx.serialization.json.Json

object Manifest : GameObjectManifest {

    override fun getCreator(wrapper: GameObjectStateWrapper): GameObjectCreator<*> = when (wrapper.typeId) {
        "character" -> Json.decodeFromString<Character.Creator>(wrapper.state)
        "dynamicBox" -> Json.decodeFromString<DynamicBox.Creator>(wrapper.state)
        "staticBox" -> Json.decodeFromString<StaticBox.Creator>(wrapper.state)
        "marker" -> Json.decodeFromString<Marker.Creator>(wrapper.state)
        else -> throw IllegalArgumentException("Unsupported type")
    }
}