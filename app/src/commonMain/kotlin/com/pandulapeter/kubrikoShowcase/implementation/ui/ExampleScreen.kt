package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.demoContentShaders.ContentShadersDemo
import com.pandulapeter.kubriko.demoContentShaders.ContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoContentShaders.createContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoInput.InputDemo
import com.pandulapeter.kubriko.demoInput.InputDemoStateHolder
import com.pandulapeter.kubriko.demoInput.createInputDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemo
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.createPerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemo
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.createPhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.ShaderAnimationsDemo
import com.pandulapeter.kubriko.demoShaderAnimations.ShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.createShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.gameSpaceSquadron.SpaceSquadronGame
import com.pandulapeter.kubriko.gameSpaceSquadron.SpaceSquadronGameStateHolder
import com.pandulapeter.kubriko.gameSpaceSquadron.createSpaceSquadronGameStateHolder
import com.pandulapeter.kubriko.gameWallbreaker.WallbreakerGame
import com.pandulapeter.kubriko.gameWallbreaker.WallbreakerGameStateHolder
import com.pandulapeter.kubriko.gameWallbreaker.createWallbreakerGameStateHolder
import com.pandulapeter.kubriko.shared.ExampleStateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry

private val currentDemoStateHolders = mutableStateOf(emptyList<ExampleStateHolder>())

@Composable
internal fun ShowcaseEntry.exampleScreen(
    shouldUseCompactUi: Boolean,
    isInFullscreenMode: Boolean,
    onFullscreenModeToggled: () -> Unit,
    getSelectedShowcaseEntry: () -> ShowcaseEntry?,
) {
    val modifier = Modifier.windowInsetsPadding(
        if (isInFullscreenMode) {
            WindowInsets.safeDrawing
        } else {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Right)
        }
    )
    when (this) {
        ShowcaseEntry.WALLBREAKER -> WallbreakerGame(
            modifier = modifier,
            stateHolder = getOrCreateState(currentDemoStateHolders, ::createWallbreakerGameStateHolder),
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.SPACE_SQUADRON -> SpaceSquadronGame(
            modifier = Modifier.windowInsetsPadding(
                if (isInFullscreenMode || shouldUseCompactUi) {
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                } else {
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Right)
                }
            ),
            stateHolder = getOrCreateState(currentDemoStateHolders, ::createSpaceSquadronGameStateHolder),
            windowInsets = if (isInFullscreenMode) {
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)
            } else {
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            },
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )

        ShowcaseEntry.CONTENT_SHADERS -> ContentShadersDemo(
            modifier = modifier,
            stateHolder = getOrCreateState(currentDemoStateHolders, ::createContentShadersDemoStateHolder),
        )

        ShowcaseEntry.INPUT -> InputDemo(
            modifier = modifier,
            stateHolder = getOrCreateState(currentDemoStateHolders, ::createInputDemoStateHolder),
        )

        ShowcaseEntry.PERFORMANCE -> PerformanceDemo(
            modifier = modifier,
            stateHolder = getOrCreateState(currentDemoStateHolders, ::createPerformanceDemoStateHolder),
        )

        ShowcaseEntry.PHYSICS -> PhysicsDemo(
            modifier = modifier,
            stateHolder = getOrCreateState(currentDemoStateHolders, ::createPhysicsDemoStateHolder),
        )

        ShowcaseEntry.SHADER_ANIMATIONS -> ShaderAnimationsDemo(
            modifier = modifier,
            stateHolder = getOrCreateState(currentDemoStateHolders, ::createShaderAnimationsDemoStateHolder),
        )
    }
    DisposableEffect(type) {
        onDispose {
            if (getSelectedShowcaseEntry() != this@exampleScreen) {
                stateHolderType.let { type ->
                    currentDemoStateHolders.value.filter { type.isInstance(it) }.forEach { it.dispose() }
                    currentDemoStateHolders.value = currentDemoStateHolders.value.filterNot { type.isInstance(it) }
                }
            }
        }
    }
}

private inline fun <reified T : ExampleStateHolder> getOrCreateState(
    stateHolders: MutableState<List<ExampleStateHolder>>,
    creator: () -> T
): T = stateHolders.value.filterIsInstance<T>().firstOrNull() ?: creator().also { stateHolders.value += it }

private val ShowcaseEntry.stateHolderType
    get() = when (this) {
        ShowcaseEntry.CONTENT_SHADERS -> ContentShadersDemoStateHolder::class
        ShowcaseEntry.SHADER_ANIMATIONS -> ShaderAnimationsDemoStateHolder::class
        ShowcaseEntry.INPUT -> InputDemoStateHolder::class
        ShowcaseEntry.PERFORMANCE -> PerformanceDemoStateHolder::class
        ShowcaseEntry.PHYSICS -> PhysicsDemoStateHolder::class
        ShowcaseEntry.SPACE_SQUADRON -> SpaceSquadronGameStateHolder::class
        ShowcaseEntry.WALLBREAKER -> WallbreakerGameStateHolder::class
    }