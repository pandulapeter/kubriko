package com.pandulapeter.kubrikoShowcase.implementation.shaders.ui

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
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ShaderDemoType
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ShadersShowcaseManager
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ui.controls.CloudControls
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ui.controls.FractalControls
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ui.controls.GradientControls
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ui.controls.WarpControls
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.collapse_controls
import kubriko.app.generated.resources.expand_controls
import kubriko.app.generated.resources.ic_brush
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val MaximumWidth = 300.dp

@Composable
internal fun ControlsContainer(
    modifier: Modifier = Modifier,
    state: Pair<ShaderDemoType, Boolean>,
    onIsExpandedChanged: (Boolean) -> Unit,
    shadersShowcaseManager: ShadersShowcaseManager,
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
                        when (targetExpanded.first) {
                            ShaderDemoType.CLOUDS -> CloudControls(
                                properties = shadersShowcaseManager.cloudState.collectAsState().value,
                                onPropertiesChanged = shadersShowcaseManager::setCloudState,
                            )

                            ShaderDemoType.FRACTAL -> FractalControls(
                                properties = shadersShowcaseManager.fractalState.collectAsState().value,
                                onPropertiesChanged = shadersShowcaseManager::setFractalState,
                            )

                            ShaderDemoType.WARP -> WarpControls(
                                properties = shadersShowcaseManager.warpState.collectAsState().value,
                                onPropertiesChanged = shadersShowcaseManager::setWarpState,
                            )

                            ShaderDemoType.GRADIENT -> GradientControls(
                                properties = shadersShowcaseManager.gradientState.collectAsState().value,
                                onPropertiesChanged = shadersShowcaseManager::setGradientState,
                            )
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