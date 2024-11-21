package com.pandulapeter.kubrikoShowcase.implementation.shaders.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ShaderDemoType
import com.pandulapeter.kubrikoShowcase.implementation.shaders.ShadersShowcaseManager
import kubriko.examples.showcase.generated.resources.Res
import kubriko.examples.showcase.generated.resources.collapse_controls
import kubriko.examples.showcase.generated.resources.expand_controls
import kubriko.examples.showcase.generated.resources.ic_brush
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
    val cardBackgroundColor = animateColorAsState(
        targetValue = if (state.second) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
        label = "cardBackgroundColor",
    )
    val iconColor = animateColorAsState(
        targetValue = if (state.second) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
        label = "card",
    )
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors().copy(containerColor = cardBackgroundColor.value),
    ) {
        Box {
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
                                properties = shadersShowcaseManager.cloudProperties.collectAsState().value,
                                onPropertiesChanged = shadersShowcaseManager::setCloudProperties,
                            )

                            ShaderDemoType.FRACTAL -> FractalControls(
                                properties = shadersShowcaseManager.fractalProperties.collectAsState().value,
                                onPropertiesChanged = shadersShowcaseManager::setFractalProperties,
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
            Icon(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .run { if (state.second) clip(CircleShape) else this }
                    .clickable { onIsExpandedChanged(!state.second) }
                    .padding(8.dp),
                painter = painterResource(Res.drawable.ic_brush),
                tint = iconColor.value,
                contentDescription = stringResource(if (state.second) Res.string.collapse_controls else Res.string.expand_controls),
            )
        }
    }
}