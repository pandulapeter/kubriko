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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.collections.immutable.toPersistentMap
import org.jetbrains.compose.resources.stringResource

@Composable
fun CustomShadersDemo(
    modifier: Modifier = Modifier,
) {
    val demoHolders = remember {
        CustomShaderDemoType.entries.associateWith {
            when (it) {
                CustomShaderDemoType.FRACTAL -> DemoHolder(FractalShader()) { shader, state -> shader.updateState(state) }
                CustomShaderDemoType.CLOUD -> DemoHolder(CloudShader()) { shader, state -> shader.updateState(state) }
                CustomShaderDemoType.WARP -> DemoHolder(WarpShader()) { shader, state -> shader.updateState(state) }
                CustomShaderDemoType.GRADIENT -> DemoHolder(GradientShader()) { shader, state -> shader.updateState(state) }
            }
        }.toPersistentMap()
    }
    val selectedDemoType = remember { mutableStateOf(CustomShaderDemoType.FRACTAL) }
    val areControlsExpanded = remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TabRow(
            modifier = Modifier.fillMaxWidth(),
            selectedTabIndex = selectedDemoType.value.ordinal,
        ) {
            CustomShaderDemoType.entries.forEach { demoType ->
                Tab(
                    modifier = Modifier.height(42.dp),
                    text = { Text(stringResource(demoType.nameStringResource)) },
                    selected = demoType == selectedDemoType.value,
                    onClick = { selectedDemoType.value = demoType }
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            AnimatedContent(
                targetState = selectedDemoType.value,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { demoType ->
                KubrikoViewport(
                    modifier = Modifier.fillMaxSize(),
                    kubriko = demoHolders[demoType]!!.kubriko,
                )
            }
            ControlsContainer(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                state = selectedDemoType.value to areControlsExpanded.value,
                onIsExpandedChanged = { areControlsExpanded.value = it },
                demoHolders = demoHolders,
            )
        }
    }
}