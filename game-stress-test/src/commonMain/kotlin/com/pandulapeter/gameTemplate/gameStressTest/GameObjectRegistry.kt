package com.pandulapeter.gameTemplate.gameStressTest

import com.pandulapeter.gameTemplate.engine.gameObject.EditorState
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.BoxWithCircle
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.Character
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.MovingBox
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

object GameObjectRegistry {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
        }
    }

    val entries = arrayOf<Triple<String, KClass<*>, (String) -> EditorState<*>>>(
        Triple(Character.TYPE_ID, Character::class) { serializedState -> json.decodeFromString<Character.CharacterState>(serializedState) },
        Triple(MovingBox.TYPE_ID, MovingBox::class) { serializedState -> json.decodeFromString<MovingBox.MovingBoxState>(serializedState) },
        Triple(BoxWithCircle.TYPE_ID, BoxWithCircle::class) { serializedState -> json.decodeFromString<BoxWithCircle.BoxWithCircleState>(serializedState) },
    )
}