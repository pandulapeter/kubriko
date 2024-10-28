package com.pandulapeter.gameTemplate.gameStressTest

import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.Character
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.DynamicBox
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.Marker
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.StaticBox
import kotlinx.serialization.json.Json

object GameObjectRegistry {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    val entries = arrayOf<Pair<String, (String) -> GameObject.State<*>>>(
        Character.TYPE_ID to { serializedState -> json.decodeFromString<Character.StateHolder>(serializedState) },
        DynamicBox.TYPE_ID to { serializedState -> json.decodeFromString<DynamicBox.StateHolder>(serializedState) },
        Marker.TYPE_ID to { serializedState -> json.decodeFromString<Marker.StateHolder>(serializedState) },
        StaticBox.TYPE_ID to { serializedState -> json.decodeFromString<StaticBox.StateHolder>(serializedState) },
    )
}