package com.pandulapeter.gameTemplate.gamePong

import com.pandulapeter.gameTemplate.engine.gameObject.EditorState
import com.pandulapeter.gameTemplate.gamePong.gameObjects.Box
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

object GameObjectRegistry {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    val entries = arrayOf<Triple<String, KClass<*>, (String) -> EditorState<*>>>(
        Triple(Box.TYPE_ID, Box::class) { serializedState -> json.decodeFromString<Box.BoxState>(serializedState) },
    )
}