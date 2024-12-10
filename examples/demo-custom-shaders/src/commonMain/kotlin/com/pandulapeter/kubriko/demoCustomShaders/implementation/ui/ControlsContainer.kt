package com.pandulapeter.kubriko.demoCustomShaders.implementation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.demoCustomShaders.implementation.CustomShaderDemoType
import com.pandulapeter.kubriko.demoCustomShaders.implementation.CustomShadersDemoManager
import com.pandulapeter.kubriko.demoCustomShaders.implementation.DemoHolder
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.FractalShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls.CloudControls
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls.FractalControls
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls.GradientControls
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls.WarpControls
import kotlinx.collections.immutable.PersistentMap
import kubriko.examples.demo_custom_shaders.generated.resources.Res
import kubriko.examples.demo_custom_shaders.generated.resources.collapse_controls
import kubriko.examples.demo_custom_shaders.generated.resources.expand_controls
import kubriko.examples.demo_custom_shaders.generated.resources.ic_brush
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val MaximumWidth = 300.dp

@Composable
internal fun ControlsContainer(
    modifier: Modifier = Modifier,
    state: Pair<CustomShaderDemoType, Boolean>,
    onIsExpandedChanged: (Boolean) -> Unit,
    demoHolders: PersistentMap<CustomShaderDemoType, DemoHolder<*, *>>
) {
    Box(
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "transformControls"
            ) { targetExpanded ->
                if (targetExpanded.second) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).width(MaximumWidth).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val manager = demoHolders[targetExpanded.first]!!.manager
                        @file:Suppress("UNCHECKED_CAST")
                        when (targetExpanded.first) {
                            CustomShaderDemoType.FRACTAL -> {
                                manager as CustomShadersDemoManager<FractalShader, FractalShader.State>
                                FractalControls(
                                    fractalShaderState = manager.shaderState.collectAsState().value,
                                    onFractalShaderStateChanged = manager::setState,
                                )
                            }

                            CustomShaderDemoType.CLOUD -> {
                                manager as CustomShadersDemoManager<CloudShader, CloudShader.State>
                                CloudControls(
                                    cloudShaderState = manager.shaderState.collectAsState().value,
                                    onCloudShaderStateChanged = manager::setState,
                                )
                            }

                            CustomShaderDemoType.WARP -> {
                                manager as CustomShadersDemoManager<WarpShader, WarpShader.State>
                                WarpControls(
                                    warpShaderState = manager.shaderState.collectAsState().value,
                                    onWarpShaderStateChanged = manager::setState,
                                )
                            }

                            CustomShaderDemoType.GRADIENT -> {
                                manager as CustomShadersDemoManager<GradientShader, GradientShader.State>
                                GradientControls(
                                    gradientShaderState = manager.shaderState.collectAsState().value,
                                    onGradientShaderStateChanged = manager::setState,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
        FloatingActionButton(
            modifier = Modifier.size(40.dp).align(Alignment.BottomEnd),
            containerColor = MaterialTheme.colorScheme.primary,
            onClick = { onIsExpandedChanged(!state.second) },
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_brush),
                contentDescription = stringResource(if (state.second) Res.string.collapse_controls else Res.string.expand_controls),
            )
        }
    }
}