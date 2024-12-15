package com.pandulapeter.kubriko.demoCustomShaders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoCustomShaders.implementation.CustomShaderDemoType
import com.pandulapeter.kubriko.demoCustomShaders.implementation.DemoHolder
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.FractalShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.ControlsContainer
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.ControlsState
import com.pandulapeter.kubriko.shared.ExampleStateHolder
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.stringResource

@Composable
fun CustomShadersDemo(
    modifier: Modifier = Modifier,
    stateHolder: CustomShadersDemoStateHolder = createCustomShadersDemoStateHolder(),
) {
    stateHolder as CustomShadersDemoStateHolderImpl
    val selectedDemoType = stateHolder.selectedDemoType.collectAsState().value
    val controlsState = stateHolder.controlsState.collectAsState().value
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TabRow(
            modifier = Modifier.fillMaxWidth(),
            selectedTabIndex = selectedDemoType.ordinal,
        ) {
            CustomShaderDemoType.entries.forEach { demoType ->
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
                    kubriko = stateHolder.demoHolders[demoType]!!.kubriko,
                )
            }
            ControlsContainer(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                state = selectedDemoType to controlsState,
                onIsExpandedChanged = stateHolder::onControlsStateChanged,
                demoHolders = stateHolder.demoHolders,
            )
        }
    }
}

sealed interface CustomShadersDemoStateHolder : ExampleStateHolder

fun createCustomShadersDemoStateHolder(): CustomShadersDemoStateHolder = CustomShadersDemoStateHolderImpl()

internal class CustomShadersDemoStateHolderImpl : CustomShadersDemoStateHolder {
    val demoHolders = CustomShaderDemoType.entries.associateWith {
        when (it) {
            CustomShaderDemoType.FRACTAL -> DemoHolder(FractalShader()) { shader, state -> shader.updateState(state) }
            CustomShaderDemoType.CLOUD -> DemoHolder(CloudShader()) { shader, state -> shader.updateState(state) }
            CustomShaderDemoType.WARP -> DemoHolder(WarpShader()) { shader, state -> shader.updateState(state) }
            CustomShaderDemoType.GRADIENT -> DemoHolder(GradientShader()) { shader, state -> shader.updateState(state) }
        }
    }.toPersistentMap()
    private val _selectedDemoType = MutableStateFlow(CustomShaderDemoType.FRACTAL)
    val selectedDemoType = _selectedDemoType.asStateFlow()
    private val _controlsState = MutableStateFlow(ControlsState.COLLAPSED)
    val controlsState = _controlsState.asStateFlow()

    fun onSelectedDemoTypeChanged(selectedDemoType: CustomShaderDemoType) = _selectedDemoType.update { selectedDemoType }

    fun onControlsStateChanged(controlsState: ControlsState) = _controlsState.update { controlsState }

    override fun dispose() = demoHolders.values.forEach { it.kubriko.dispose() }
}