package com.pandulapeter.gameTemplate.engine.implementation.managers

import com.pandulapeter.gameTemplate.engine.gameObject.State
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.managers.SerializationManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class SerializationManagerImpl : SerializationManager {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun serializeGameObjectStates(
        gameObjectStates: List<State<*>>,
    ) = json.encodeToString(
        gameObjectStates.map { state ->
            GameObjectStateWrapper(
                typeId = state.typeId,
                serializedState = state.serialize(),
            )
        }
    )

    override suspend fun deserializeGameObjectStates(
        serializedStates: String,
    ) = json.decodeFromString<List<GameObjectStateWrapper>>(serializedStates).mapNotNull { wrapper ->
        EngineImpl.gameObjectManager.gameObjectStateRegistry.value[wrapper.typeId]?.invoke(wrapper.serializedState)
    }

    @Serializable
    private data class GameObjectStateWrapper(
        @SerialName("typeId") val typeId: String,
        @SerialName("state") val serializedState: String,
    )
}