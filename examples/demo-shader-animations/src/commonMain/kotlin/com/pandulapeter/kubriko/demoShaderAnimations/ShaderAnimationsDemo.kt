package com.pandulapeter.kubriko.demoShaderAnimations

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationDemoHolder
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationDemoType
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.EtherShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.NoodleShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.ControlsContainer
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.ControlsState
import com.pandulapeter.kubriko.shared.ExampleStateHolder
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShaderAnimationsDemo(
    modifier: Modifier = Modifier,
    stateHolder: ShaderAnimationsDemoStateHolder = createShaderAnimationsDemoStateHolder(),
) {
    stateHolder as ShaderAnimationsDemoStateHolderImpl
    val selectedDemoType = stateHolder.selectedDemoType.collectAsState().value
    val controlsState = stateHolder.controlsState.collectAsState().value
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        ScrollableTabRow(
            edgePadding = 0.dp,
            selectedTabIndex = selectedDemoType.ordinal,
            divider = {},
        ) {
            ShaderAnimationDemoType.entries.forEach { demoType ->
                Tab(
                    modifier = Modifier.height(42.dp),
                    text = { Text(stringResource(demoType.nameStringResource)) },
                    selected = demoType == selectedDemoType,
                    onClick = { stateHolder.onSelectedDemoTypeChanged(demoType) }
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            AnimatedContent(
                targetState = selectedDemoType,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { demoType ->
                KubrikoViewport(
                    modifier = Modifier.fillMaxSize(),
                    kubriko = stateHolder.shaderAnimationDemoHolders[demoType]!!.kubriko,
                )
            }
            ControlsContainer(
                modifier = modifier.align(Alignment.BottomEnd).padding(16.dp),
                state = selectedDemoType to controlsState,
                onIsExpandedChanged = stateHolder::onControlsStateChanged,
                shaderAnimationDemoHolders = stateHolder.shaderAnimationDemoHolders,
            )
        }
    }
}

sealed interface ShaderAnimationsDemoStateHolder : ExampleStateHolder

fun createShaderAnimationsDemoStateHolder(): ShaderAnimationsDemoStateHolder = ShaderAnimationsDemoStateHolderImpl()

internal class ShaderAnimationsDemoStateHolderImpl : ShaderAnimationsDemoStateHolder {
    val shaderAnimationDemoHolders = ShaderAnimationDemoType.entries.associateWith {
        when (it) {
            ShaderAnimationDemoType.GRADIENT -> ShaderAnimationDemoHolder(GradientShader()) { shader, state -> shader.updateState(state) }
            ShaderAnimationDemoType.NOODLE -> ShaderAnimationDemoHolder(NoodleShader()) { shader, state -> shader.updateState(state) }
            ShaderAnimationDemoType.ETHER -> ShaderAnimationDemoHolder(EtherShader()) { shader, state -> shader.updateState(state) }
            ShaderAnimationDemoType.CLOUD -> ShaderAnimationDemoHolder(CloudShader()) { shader, state -> shader.updateState(state) }
            ShaderAnimationDemoType.WARP -> ShaderAnimationDemoHolder(WarpShader()) { shader, state -> shader.updateState(state) }
        }
    }.toPersistentMap()
    private val _selectedDemoType = MutableStateFlow(ShaderAnimationDemoType.entries.first())
    val selectedDemoType = _selectedDemoType.asStateFlow()
    private val _controlsState = MutableStateFlow(ControlsState.COLLAPSED)
    val controlsState = _controlsState.asStateFlow()

    fun onSelectedDemoTypeChanged(selectedDemoType: ShaderAnimationDemoType) = _selectedDemoType.update { selectedDemoType }

    fun onControlsStateChanged(controlsState: ControlsState) = _controlsState.update { controlsState }

    override fun dispose() = shaderAnimationDemoHolders.values.forEach { it.kubriko.dispose() }
}