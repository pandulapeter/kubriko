/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui

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
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationDemoHolder
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ShaderAnimationDemoType
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.managers.ShaderAnimationsDemoManager
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.EtherShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.NoodleShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.controls.CloudControls
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.controls.EtherControls
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.controls.GradientControls
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.controls.NoodleControls
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.controls.WarpControls
import com.pandulapeter.kubriko.uiComponents.FloatingButton
import kotlinx.collections.immutable.PersistentMap
import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.collapse_controls
import kubriko.examples.demo_shader_animations.generated.resources.expand_controls
import kubriko.examples.demo_shader_animations.generated.resources.hide_code
import kubriko.examples.demo_shader_animations.generated.resources.ic_brush
import kubriko.examples.demo_shader_animations.generated.resources.ic_code
import kubriko.examples.demo_shader_animations.generated.resources.show_code
import org.jetbrains.compose.resources.stringResource

private val MaximumWidth = 300.dp

@Composable
internal fun ControlsContainer(
    modifier: Modifier = Modifier,
    state: Pair<ShaderAnimationDemoType, ControlsState>,
    onIsExpandedChanged: (ControlsState) -> Unit,
    shaderAnimationDemoHolders: PersistentMap<ShaderAnimationDemoType, ShaderAnimationDemoHolder<*, *>>
) = Box(
    modifier = modifier,
) {
    val cardAlpha: Float by animateFloatAsState(
        targetValue = if (state.second == ControlsState.COLLAPSED) 0f else 1f,
        animationSpec = tween(),
    )
    val cardEndPaddingMultiplier: Float by animateFloatAsState(
        targetValue = if (state.second == ControlsState.EXPANDED_CODE) 1f else 0f,
        animationSpec = tween(),
    )
    Card(
        modifier = Modifier.padding(16.dp).alpha(cardAlpha).padding(end = 48.dp * cardEndPaddingMultiplier),
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentAlignment = Alignment.TopStart,
            label = "transformControls"
        ) { targetState ->
            when (targetState.second) {
                ControlsState.COLLAPSED -> Unit
                ControlsState.EXPANDED_CODE -> Code(
                    demoType = targetState.first,
                )

                ControlsState.EXPANDED_CONTROLS -> Controls(
                    manager = shaderAnimationDemoHolders[targetState.first]!!.shaderAnimationsDemoManager,
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
    demoType: ShaderAnimationDemoType,
) = Text(
    modifier = Modifier
        .verticalScroll(rememberScrollState())
        .horizontalScroll(rememberScrollState())
        .padding(
            horizontal = 16.dp,
        ),
    style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Light,
        fontFamily = FontFamily.Monospace,
    ),
    text = when (demoType) {
        ShaderAnimationDemoType.CLOUD -> CloudShader.CODE
        ShaderAnimationDemoType.ETHER -> EtherShader.CODE
        ShaderAnimationDemoType.GRADIENT -> GradientShader.CODE
        ShaderAnimationDemoType.NOODLE -> NoodleShader.CODE
        ShaderAnimationDemoType.WARP -> WarpShader.CODE
    }
)

@Composable
private fun Controls(
    manager: ShaderAnimationsDemoManager<*, *>,
    demoType: ShaderAnimationDemoType,
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
        ShaderAnimationDemoType.CLOUD -> {
            manager as ShaderAnimationsDemoManager<CloudShader, CloudShader.State>
            CloudControls(
                cloudShaderState = manager.shaderState.collectAsState().value,
                onCloudShaderStateChanged = manager::setState,
            )
        }

        ShaderAnimationDemoType.ETHER -> {
            manager as ShaderAnimationsDemoManager<EtherShader, EtherShader.State>
            EtherControls(
                etherShaderState = manager.shaderState.collectAsState().value,
                onEtherShaderStateChanged = manager::setState,
            )
        }

        ShaderAnimationDemoType.GRADIENT -> {
            manager as ShaderAnimationsDemoManager<GradientShader, GradientShader.State>
            GradientControls(
                gradientShaderState = manager.shaderState.collectAsState().value,
                onGradientShaderStateChanged = manager::setState,
            )
        }

        ShaderAnimationDemoType.NOODLE -> {
            manager as ShaderAnimationsDemoManager<NoodleShader, NoodleShader.State>
            NoodleControls(
                noodleShaderState = manager.shaderState.collectAsState().value,
                onNoodleShaderStateChanged = manager::setState,
            )
        }

        ShaderAnimationDemoType.WARP -> {
            manager as ShaderAnimationsDemoManager<WarpShader, WarpShader.State>
            WarpControls(
                warpShaderState = manager.shaderState.collectAsState().value,
                onWarpShaderStateChanged = manager::setState,
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