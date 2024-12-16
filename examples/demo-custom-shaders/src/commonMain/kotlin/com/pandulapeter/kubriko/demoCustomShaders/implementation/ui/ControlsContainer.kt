package com.pandulapeter.kubriko.demoCustomShaders.implementation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.pandulapeter.kubriko.shared.ui.FloatingButton
import kotlinx.collections.immutable.PersistentMap
import kubriko.examples.demo_custom_shaders.generated.resources.Res
import kubriko.examples.demo_custom_shaders.generated.resources.collapse_controls
import kubriko.examples.demo_custom_shaders.generated.resources.expand_controls
import kubriko.examples.demo_custom_shaders.generated.resources.hide_code
import kubriko.examples.demo_custom_shaders.generated.resources.ic_brush
import kubriko.examples.demo_custom_shaders.generated.resources.ic_code
import kubriko.examples.demo_custom_shaders.generated.resources.show_code
import org.jetbrains.compose.resources.stringResource

private val MaximumWidth = 300.dp

@Composable
internal fun ControlsContainer(
    modifier: Modifier = Modifier,
    state: Pair<CustomShaderDemoType, ControlsState>,
    onIsExpandedChanged: (ControlsState) -> Unit,
    demoHolders: PersistentMap<CustomShaderDemoType, DemoHolder<*, *>>
) = Box(
    modifier = modifier,
) {
    val cardAlpha: Float by animateFloatAsState(
        targetValue = if (state.second == ControlsState.COLLAPSED) 0f else 1f,
        animationSpec = tween(),
    )
    Card(
        modifier = Modifier.padding(16.dp).alpha(cardAlpha),
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "transformControls"
        ) { targetState ->
            when (targetState.second) {
                ControlsState.COLLAPSED -> Unit
                ControlsState.EXPANDED_CODE -> Code(
                    demoType = targetState.first,
                )

                ControlsState.EXPANDED_CONTROLS -> Controls(
                    manager = demoHolders[targetState.first]!!.manager,
                    demoType = targetState.first,
                )
            }
        }
    }
    Row(
        modifier = Modifier.align(Alignment.BottomEnd),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (state.second == ControlsState.EXPANDED_CODE).let { isSelected ->
            FloatingButton(
                icon = Res.drawable.ic_code,
                isSelected = isSelected,
                contentDescription = stringResource(if (isSelected) Res.string.hide_code else Res.string.show_code),
                onButtonPressed = { onIsExpandedChanged(if (isSelected) ControlsState.COLLAPSED else ControlsState.EXPANDED_CODE) },
            )
        }
        (state.second == ControlsState.EXPANDED_CONTROLS).let { isSelected ->
            FloatingButton(
                icon = Res.drawable.ic_brush,
                isSelected = isSelected,
                contentDescription = stringResource(if (isSelected) Res.string.collapse_controls else Res.string.expand_controls),
                onButtonPressed = { onIsExpandedChanged(if (isSelected) ControlsState.COLLAPSED else ControlsState.EXPANDED_CONTROLS) },
            )
        }
    }
}

@Composable
private fun Code(
    demoType: CustomShaderDemoType,
) = Text(
    modifier = Modifier
        .verticalScroll(rememberScrollState())
        .horizontalScroll(rememberScrollState())
        .padding(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
    style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Light,
        fontFamily = FontFamily.Monospace,
    ),
    text = when (demoType) {
        CustomShaderDemoType.FRACTAL -> FractalShader.CODE
        CustomShaderDemoType.CLOUD -> CloudShader.CODE
        CustomShaderDemoType.WARP -> WarpShader.CODE
        CustomShaderDemoType.GRADIENT -> GradientShader.CODE
    }
)

@Composable
private fun Controls(
    manager: CustomShadersDemoManager<*, *>,
    demoType: CustomShaderDemoType,
) = Column(
    modifier = Modifier
        .verticalScroll(rememberScrollState())
        .width(MaximumWidth)
        .padding(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    @file:Suppress("UNCHECKED_CAST")
    when (demoType) {
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


internal enum class ControlsState {
    COLLAPSED,
    EXPANDED_CODE,
    EXPANDED_CONTROLS;
}