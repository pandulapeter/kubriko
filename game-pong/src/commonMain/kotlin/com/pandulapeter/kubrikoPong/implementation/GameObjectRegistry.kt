package com.pandulapeter.kubrikoPong.implementation

import com.pandulapeter.kubriko.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.kubrikoPong.implementation.gameObjects.Box
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

object GameObjectRegistry {
    private val json by lazy { Json { ignoreUnknownKeys = true } }

    val typesAvailableInEditor = arrayOf<Triple<String, KClass<*>, (String) -> AvailableInEditor.State<*>>>(
        Triple(Box.TYPE_ID, Box::class) { serializedState -> json.decodeFromString<Box.BoxState>(serializedState) },
    )
}