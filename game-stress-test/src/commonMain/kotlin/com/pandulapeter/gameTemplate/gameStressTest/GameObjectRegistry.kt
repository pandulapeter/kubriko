package com.pandulapeter.gameTemplate.gameStressTest

import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.Character
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.DynamicBox
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.StaticBox
import kotlinx.serialization.json.Json

object GameObjectRegistry {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    val entries = arrayOf<Pair<String, (String) -> GameObject.Serializer<*>>>(
        Character.TYPE_ID to { serializedState -> json.decodeFromString<Character.SerializerHolder>(serializedState) },
        DynamicBox.TYPE_ID to { serializedState -> json.decodeFromString<DynamicBox.SerializerHolder>(serializedState) },
        StaticBox.TYPE_ID to { serializedState -> json.decodeFromString<StaticBox.SerializerHolder>(serializedState) },
    )
}