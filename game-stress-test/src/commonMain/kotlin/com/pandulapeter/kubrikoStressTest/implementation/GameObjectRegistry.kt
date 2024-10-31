package com.pandulapeter.kubrikoStressTest.implementation

import com.pandulapeter.kubriko.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.BoxWithCircle
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.Character
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.MovingBox
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

object GameObjectRegistry {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    val typesAvailableInEditor = arrayOf<Triple<String, KClass<*>, (String) -> AvailableInEditor.State<*>>>(
        Triple(Character.TYPE_ID, Character::class) { serializedState -> json.decodeFromString<Character.CharacterState>(serializedState) },
        Triple(MovingBox.TYPE_ID, MovingBox::class) { serializedState -> json.decodeFromString<MovingBox.MovingBoxState>(serializedState) },
        Triple(BoxWithCircle.TYPE_ID, BoxWithCircle::class) { serializedState -> json.decodeFromString<BoxWithCircle.BoxWithCircleState>(serializedState) },
    )
}