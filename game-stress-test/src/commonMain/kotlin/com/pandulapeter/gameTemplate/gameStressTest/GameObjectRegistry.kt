package com.pandulapeter.gameTemplate.gameStressTest

import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.Character
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.MovingBox
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.BoxWithCircle
import kotlinx.serialization.json.Json

object GameObjectRegistry {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    val entries = arrayOf<Pair<String, (String) -> Serializer<*>>>(
        Character.TYPE_ID to { serializedState -> json.decodeFromString<Character.State>(serializedState) },
        MovingBox.TYPE_ID to { serializedState -> json.decodeFromString<MovingBox.State>(serializedState) },
        BoxWithCircle.TYPE_ID to { serializedState -> json.decodeFromString<BoxWithCircle.State>(serializedState) },
    )
}