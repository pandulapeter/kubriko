package com.pandulapeter.kubrikoStressTest.implementation

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.implementation.extensions.isAroundPosition
import com.pandulapeter.kubriko.keyboardInputManager.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInputManager.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.shaderManager.ShaderManager
import com.pandulapeter.kubriko.shaderManager.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shaderManager.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shaderManager.collection.VignetteShader
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubrikoStressTest.implementation.actors.KeyboardInputListener
import com.pandulapeter.kubrikoStressTest.implementation.actors.traits.Destructible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kubriko.games.stress_test.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal object GameplayController : CoroutineScope, KeyboardInputAware {

    const val SCENE_NAME = "scene_stress_test"

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val kubriko = Kubriko.newInstance(
        KeyboardInputManager.newInstance(),
        ShaderManager.newInstance(),
    )
    private val sceneSerializer by lazy { SceneSerializerWrapper().sceneSerializer }
    private val actorManager by lazy { kubriko.get<ActorManager>() }
    val stateManager by lazy { kubriko.get<StateManager>() }
    val viewportManager by lazy { kubriko.get<ViewportManager>() }

    init {
        stateManager.isFocused
            .filterNot { it }
            .onEach { stateManager.updateIsRunning(false) }
            .launchIn(this)
        loadMap(SCENE_NAME)
    }

    override fun onKeyReleased(key: Key) = when (key) {
        Key.Escape, Key.Back, Key.Backspace -> stateManager.updateIsRunning(!stateManager.isRunning.value)
        else -> Unit
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadMap(mapName: String) = launch {
        try {
            val actors = listOf(
                ChromaticAberrationShader(),
                VignetteShader(),
                SmoothPixelationShader(),
                KeyboardInputListener(stateManager),
            ) + sceneSerializer.deserializeActors(Res.readBytes("files/scenes/$mapName.json").decodeToString())
            actorManager.add(actors = actors.toTypedArray())
        } catch (_: MissingResourceException) {
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