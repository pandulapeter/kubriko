package com.pandulapeter.gameTemplate.engine.implementation.managers

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectManifest
import com.pandulapeter.gameTemplate.engine.gameObject.GameObjectStateWrapper
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isAroundPosition
import com.pandulapeter.gameTemplate.engine.implementation.extensions.isVisible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.occupiesPosition
import com.pandulapeter.gameTemplate.engine.managers.GameObjectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class GameObjectManagerImpl : GameObjectManager {

    val gameObjects = MutableStateFlow(emptyList<GameObject<*>>())
    val dynamicGameObjects = gameObjects.map { it.filterIsInstance<Dynamic>() }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    private val visibleGameObjects = gameObjects.map { it.filterIsInstance<Visible>() }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())
    val visibleGameObjectsInViewport = combine(
        EngineImpl.metadataManager.runtimeInMilliseconds.map { it / 100 }.distinctUntilChanged(),
        visibleGameObjects,
        EngineImpl.viewportManager.size,
        EngineImpl.viewportManager.offset,
        EngineImpl.viewportManager.scaleFactor,
    ) { _, allVisibleGameObjects, viewportSize, viewportOffset, viewportScaleFactor ->
        allVisibleGameObjects
            .filter {
                it.isVisible(
                    scaledHalfViewportSize = viewportSize / (viewportScaleFactor * 2),
                    viewportOffset = viewportOffset,
                    viewportScaleFactor = viewportScaleFactor,
                )
            }
            .sortedByDescending { it.depth }
    }.stateIn(EngineImpl, SharingStarted.Eagerly, emptyList())

    override fun add(gameObject: GameObject<*>) = gameObjects.update { currentValue ->
        currentValue + gameObject
    }

    override fun add(gameObjects: Collection<GameObject<*>>) = EngineImpl.gameObjectManager.gameObjects.update { currentValue ->
        currentValue + gameObjects
    }

    override suspend fun addFromJson(json: String, manifest: GameObjectManifest) {
        removeAll()
        add(
            Json.decodeFromString<List<GameObjectStateWrapper>>(json).map { wrapper ->
                manifest.deserializeState(wrapper).instantiate()
            }
        )
    }

    override fun remove(gameObject: GameObject<*>) = gameObjects.update { currentValue ->
        currentValue.filterNot { it == gameObject }
    }

    override fun remove(gameObjects: Collection<GameObject<*>>) = EngineImpl.gameObjectManager.gameObjects.update { currentValue ->
        currentValue.filterNot { it in gameObjects }
    }

    override fun removeAll() = gameObjects.update { emptyList() }

    override suspend fun saveToJson() = Json.encodeToString(
        gameObjects.value.map { gameObject ->
            GameObjectStateWrapper(
                typeId = gameObject.typeId,
                serializedState = gameObject.getState().serialize(),
            )
        }
    )

    override fun findGameObjectsWithBoundsInPosition(position: Offset) = visibleGameObjectsInViewport.value
        .filter { it.occupiesPosition(position) }

    override fun findGameObjectsWithPivotsAroundPosition(position: Offset, range: Float) = visibleGameObjects.value
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }
}