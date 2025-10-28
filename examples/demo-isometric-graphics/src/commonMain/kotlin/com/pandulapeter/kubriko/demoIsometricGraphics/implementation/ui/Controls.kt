/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.managers.GridManager
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.managers.IsometricGraphicsDemoManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import kubriko.examples.demo_isometric_graphics.generated.resources.Res
import kubriko.examples.demo_isometric_graphics.generated.resources.bounce
import kubriko.examples.demo_isometric_graphics.generated.resources.character
import kubriko.examples.demo_isometric_graphics.generated.resources.debug_bounds
import kubriko.examples.demo_isometric_graphics.generated.resources.environment
import kubriko.examples.demo_isometric_graphics.generated.resources.movement
import kubriko.examples.demo_isometric_graphics.generated.resources.orientation
import kubriko.examples.demo_isometric_graphics.generated.resources.reset_camera
import kubriko.examples.demo_isometric_graphics.generated.resources.section_world
import kubriko.examples.demo_isometric_graphics.generated.resources.spin
import kubriko.examples.demo_isometric_graphics.generated.resources.tile_height
import kubriko.examples.demo_isometric_graphics.generated.resources.tile_width
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Controls(
    modifier: Modifier = Modifier,
    gridManager: GridManager,
    isometricWorldViewportManager: ViewportManager,
    isometricGraphicsDemoManager: IsometricGraphicsDemoManager,
) = Column(
    modifier = modifier
        .verticalScroll(rememberScrollState())
        .sizeIn(maxWidth = 240.dp)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Right))
        .padding(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    SectionHeader(
        title = Res.string.section_world,
    )
    CustomButton(
        modifier = Modifier.fillMaxWidth(),
        label = Res.string.reset_camera,
        onClick = {
            gridManager.tileWidthMultiplier.value = 1f
            gridManager.tileHeightMultiplier.value = 0.5f
            isometricWorldViewportManager.setCameraPosition(SceneOffset.Zero)
        },
    )
    SliderWithText(
        label = Res.string.tile_width,
        valueRange = GridManager.ZOOM_MINIMUM..GridManager.ZOOM_MAXIMUM,
        value = gridManager.tileWidthMultiplier.collectAsState().value,
        onValueChanged = { gridManager.tileWidthMultiplier.value = it },
    )
    SliderWithText(
        label = Res.string.tile_height,
        valueRange = GridManager.ZOOM_MINIMUM..GridManager.ZOOM_MAXIMUM,
        value = gridManager.tileHeightMultiplier.collectAsState().value,
        onValueChanged = { gridManager.tileHeightMultiplier.value = it },
    )
    SwitchWithText(
        label = Res.string.debug_bounds,
        isChecked = isometricGraphicsDemoManager.shouldDrawDebugBounds.collectAsState().value,
        onCheckedChanged = { isometricGraphicsDemoManager.shouldDrawDebugBounds.value = it },
    )
    HorizontalDivider()
    SectionHeader(
        title = Res.string.environment,
    )
    SwitchWithText(
        label = Res.string.spin,
        isChecked = isometricGraphicsDemoManager.shouldRotate.collectAsState().value,
        onCheckedChanged = { isometricGraphicsDemoManager.shouldRotate.value = it },
    )
    SwitchWithText(
        label = Res.string.bounce,
        isChecked = isometricGraphicsDemoManager.shouldBounce.collectAsState().value,
        onCheckedChanged = { isometricGraphicsDemoManager.shouldBounce.value = it },
    )
    HorizontalDivider()
    SectionHeader(
        title = Res.string.character,
    )
    SliderWithText(
        label = Res.string.orientation,
        valueRange = -1f..1f,
        value = isometricGraphicsDemoManager.characterOrientation.collectAsState().value,
        onValueChanged = { isometricGraphicsDemoManager.characterOrientation.value = it },
    )
    SwitchWithText(
        label = Res.string.movement,
        isChecked = isometricGraphicsDemoManager.shouldMove.collectAsState().value,
        onCheckedChanged = { isometricGraphicsDemoManager.shouldMove.value = it },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderWithText(
    modifier: Modifier = Modifier,
    label: StringResource,
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    isEnabled: Boolean = true,
) = Column(
    modifier = modifier.padding(horizontal = 8.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    Label(
        label = stringResource(label),
    )
    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors().copy(
        inactiveTrackColor = MaterialTheme.colorScheme.primary,
    )
    Slider(
        modifier = Modifier.height(24.dp),
        value = value,
        onValueChange = onValueChanged,
        valueRange = valueRange,
        thumb = {
            Spacer(
                Modifier
                    .size(4.dp, 16.dp)
                    .hoverable(interactionSource = interactionSource)
                    .background(if (isEnabled) colors.thumbColor else colors.disabledThumbColor, CircleShape)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                modifier = Modifier.height(4.dp),
                colors = colors,
                enabled = isEnabled,
                sliderState = sliderState,
                trackInsideCornerSize = 0.dp,
                thumbTrackGapSize = 0.dp,
            )
        }
    )
}

@Composable
internal fun SwitchWithText(
    modifier: Modifier = Modifier,
    label: StringResource,
    isChecked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    isEnabled: Boolean = true,
) = Row(
    modifier = modifier.selectable(
        selected = isChecked,
        onClick = { onCheckedChanged(!isChecked) }
    ).padding(start = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    Label(
        modifier = Modifier.weight(1f),
        label = stringResource(label),
    )
    Switch(
        modifier = Modifier.scale(0.6f).height(24.dp),
        checked = isChecked,
        onCheckedChange = onCheckedChanged,
        enabled = isEnabled,
    )
}

@Composable
private fun CustomButton(
    modifier: Modifier = Modifier,
    label: StringResource,
    onClick: () -> Unit,
) = Button(
    modifier = modifier
        .height(32.dp)
        .padding(
            horizontal = 8.dp,
            vertical = 4.dp,
        ),
    onClick = onClick,
    shape = RectangleShape,
    contentPadding = PaddingValues(
        horizontal = 8.dp,
        vertical = 2.dp,
    ),
) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun Label(
    modifier: Modifier = Modifier,
    label: String,
) = Text(
    modifier = modifier,
    text = label,
    style = MaterialTheme.typography.labelSmall,
)

@Composable
private fun SectionHeader(
    modifier: Modifier = Modifier,
    title: StringResource,
) = Text(
    modifier = modifier.padding(
        horizontal = 8.dp,
        vertical = 4.dp,
    ),
    text = stringResource(title),
    color = MaterialTheme.colorScheme.primary,
    style = MaterialTheme.typography.labelMedium,
    fontWeight = FontWeight.Bold,
)