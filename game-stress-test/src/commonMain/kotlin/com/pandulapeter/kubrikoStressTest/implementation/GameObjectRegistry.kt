package com.pandulapeter.kubrikoStressTest.implementation

import com.pandulapeter.kubriko.engine.actor.ActorRegistrationForEditor
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.BoxWithCircle
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.Character
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.MovingBox
import kotlinx.serialization.json.Json

object GameObjectRegistry {
    private val json by lazy { Json { ignoreUnknownKeys = true } }
    val typesAvailableInEditor = arrayOf(
        ActorRegistrationForEditor(Character.TYPE_ID) { serializedState -> json.decodeFromString<Character.CharacterState>(serializedState) },
        ActorRegistrationForEditor(MovingBox.TYPE_ID) { serializedState -> json.decodeFromString<MovingBox.MovingBoxState>(serializedState) },
        ActorRegistrationForEditor(BoxWithCircle.TYPE_ID) { serializedState -> json.decodeFromString<BoxWithCircle.BoxWithCircleState>(serializedState) },
    )
}