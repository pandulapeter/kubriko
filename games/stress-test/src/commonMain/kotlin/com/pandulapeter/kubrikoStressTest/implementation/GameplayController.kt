package com.pandulapeter.kubrikoStressTest.implementation

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.KeyboardZoomState
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.implementation.extensions.isAroundPosition
import com.pandulapeter.kubriko.implementation.extensions.zoomState
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.InputManager
import com.pandulapeter.kubriko.manager.ShaderManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.shader.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shader.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shader.collection.VignetteShader
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubrikoStressTest.implementation.actors.traits.Destructible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kubriko.games.stress_test.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal object GameplayController : CoroutineScope {

    const val SCENE_NAME = "scene_stress_test"

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val kubriko = Kubriko.newInstance(
        ShaderManager.newInstance()
    )
    private val sceneSerializer by lazy { SceneSerializerWrapper().sceneSerializer }
    private val actorManager by lazy { kubriko.get<ActorManager>() }
    private val inputManager by lazy { kubriko.get<InputManager>() }
    val stateManager by lazy { kubriko.get<StateManager>() }
    val viewportManager by lazy { kubriko.get<ViewportManager>() }

    init {
        stateManager.isFocused
            .filterNot { it }
            .onEach { stateManager.updateIsRunning(false) }
            .launchIn(this)
        inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(::handleKeys)
            .launchIn(this)
        inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
        loadMap(SCENE_NAME)
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadMap(mapName: String) = launch {
        try {
            kubriko.get<ActorManager>().run {
                add(ChromaticAberrationShader())
                add(VignetteShader())
                add(SmoothPixelationShader())
                add(actors = sceneSerializer.deserializeActors(Res.readBytes("files/scenes/$mapName.json").decodeToString()).toTypedArray())
            }
        } catch (_: MissingResourceException) {
        }
    }

    private fun handleKeys(keys: Set<Key>) {
        if (stateManager.isRunning.value) {
            viewportManager.multiplyScaleFactor(
                when (keys.zoomState) {
                    KeyboardZoomState.NONE -> 1f
                    KeyboardZoomState.ZOOM_IN -> 1.02f
                    KeyboardZoomState.ZOOM_OUT -> 0.98f
                }
            )
        }
    }

    private fun handleKeyReleased(key: Key) {
        when (key) {
            Key.Escape, Key.Back, Key.Backspace -> stateManager.updateIsRunning(!stateManager.isRunning.value)
        }
    }

    fun findDestructibleActorsNearby(
        position: SceneOffset,
        range: Float,
    ) = actorManager.allActors.value
        .filterIsInstance<Destructible>()
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }
}