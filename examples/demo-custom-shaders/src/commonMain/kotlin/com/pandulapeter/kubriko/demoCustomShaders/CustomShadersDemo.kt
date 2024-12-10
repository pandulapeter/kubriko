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
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoCustomShaders.implementation.CustomShaderDemoType
import com.pandulapeter.kubriko.demoCustomShaders.implementation.CustomShadersDemoManager
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.FractalShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.ControlsContainer
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import org.jetbrains.compose.resources.stringResource

@Composable
fun CustomShadersDemo(
    modifier: Modifier = Modifier,
) {
    val kubrikoFractal = remember { createKubrikoInstance(FractalShader()) { shader, state -> shader.updateState(state) } }
    val kubrikoCloud = remember { createKubrikoInstance(CloudShader()) { shader, state -> shader.updateState(state) } }
    val kubrikoWarp = remember { createKubrikoInstance(WarpShader()) { shader, state -> shader.updateState(state) } }
    val kubrikoGradient = remember { createKubrikoInstance(GradientShader()) { shader, state -> shader.updateState(state) } }
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
                    kubriko = when (demoType) {
                        CustomShaderDemoType.FRACTAL -> kubrikoFractal
                        CustomShaderDemoType.CLOUD -> kubrikoCloud
                        CustomShaderDemoType.WARP -> kubrikoWarp
                        CustomShaderDemoType.GRADIENT -> kubrikoGradient
                    },
                )
            }
            ControlsContainer(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                state = selectedDemoType.value to areControlsExpanded.value,
                onIsExpandedChanged = { areControlsExpanded.value = it },
                getFractalShaderState = { kubrikoFractal.require<CustomShadersDemoManager<FractalShader, FractalShader.State>>().shaderState },
                onFractalShaderStateChanged = kubrikoFractal.require<CustomShadersDemoManager<FractalShader, FractalShader.State>>()::setState,
                getCloudShaderState = { kubrikoCloud.require<CustomShadersDemoManager<CloudShader, CloudShader.State>>().shaderState },
                onCloudShaderStateChanged = kubrikoCloud.require<CustomShadersDemoManager<CloudShader, CloudShader.State>>()::setState,
                getWarpShaderState = { kubrikoWarp.require<CustomShadersDemoManager<WarpShader, WarpShader.State>>().shaderState },
                onWarpShaderStateChanged = kubrikoWarp.require<CustomShadersDemoManager<WarpShader, WarpShader.State>>()::setState,
                getGradientShaderState = { kubrikoGradient.require<CustomShadersDemoManager<GradientShader, GradientShader.State>>().shaderState },
                onGradientShaderStateChanged = kubrikoGradient.require<CustomShadersDemoManager<GradientShader, GradientShader.State>>()::setState,
            )
        }
    }
}

private fun <SHADER : Shader<STATE>, STATE : Shader.State> createKubrikoInstance(
    shader: SHADER,
    updater: (SHADER, STATE) -> Unit,
) = Kubriko.newInstance(
    ShaderManager.newInstance(),
    CustomShadersDemoManager(shader, updater),
)