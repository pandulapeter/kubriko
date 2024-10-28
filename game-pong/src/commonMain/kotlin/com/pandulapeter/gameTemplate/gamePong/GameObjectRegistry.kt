package com.pandulapeter.gameTemplate.gamePong

import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.gamePong.gameObjects.Box
import kotlinx.serialization.json.Json

object GameObjectRegistry {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    val entries = arrayOf<Pair<String, (String) -> Serializer<*>>>(
        Box.TYPE_ID to { serializedState -> json.decodeFromString<Box.State>(serializedState) },
    )
}