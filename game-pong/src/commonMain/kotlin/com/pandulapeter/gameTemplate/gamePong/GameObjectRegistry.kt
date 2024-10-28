package com.pandulapeter.gameTemplate.gamePong

import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.gamePong.gameObjects.Box
import com.pandulapeter.gameTemplate.gamePong.gameObjects.Character
import kotlinx.serialization.json.Json

object GameObjectRegistry {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    val entries = arrayOf<Pair<String, (String) -> GameObject.State<*>>>(
        Character.TYPE_ID to { serializedState -> json.decodeFromString<Character.StateHolder>(serializedState) },
        Box.TYPE_ID to { serializedState -> json.decodeFromString<Box.StateHolder>(serializedState) },
    )
}