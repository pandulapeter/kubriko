package com.pandulapeter.kubriko.gameSpaceSquadron

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronBackgroundManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronGameManager
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder

@Composable
fun SpaceSquadronGame(
    modifier: Modifier = Modifier,
    stateHolder: SpaceSquadronGameStateHolder = createSpaceSquadronGameStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as SpaceSquadronGameStateHolderImpl
    KubrikoViewport(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        kubriko = stateHolder.backgroundKubriko,
    ) {
        KubrikoViewport(
            modifier = modifier,
            kubriko = stateHolder.kubriko,
            windowInsets = windowInsets,
        )
    }
}

sealed interface SpaceSquadronGameStateHolder : ExampleStateHolder

fun createSpaceSquadronGameStateHolder(): SpaceSquadronGameStateHolder = SpaceSquadronGameStateHolderImpl()

private class SpaceSquadronGameStateHolderImpl : SpaceSquadronGameStateHolder {
    val stateManager = StateManager.newInstance(shouldAutoStart = false)
    val backgroundKubriko = Kubriko.newInstance(
        ShaderManager.newInstance(),
        SpaceSquadronBackgroundManager()
    )
    val kubriko = Kubriko.newInstance(
        stateManager,
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(
                height = ViewportHeight,
            )
        ),
        CollisionManager.newInstance(),
        KeyboardInputManager.newInstance(),
        PointerInputManager.newInstance(isActiveAboveViewport = true),
        SpaceSquadronGameManager(),
    )

    override fun dispose() = kubriko.dispose()
}

internal val ViewportHeight = 1280.sceneUnit