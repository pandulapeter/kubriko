package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorSlider(
    modifier: Modifier = Modifier,
    title: String = "",
    suffix: String = "",
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>? = null,
    enabled: Boolean = true,
) = Column(
    modifier = modifier,
) {
    val add = remember { mutableStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    if (title.isNotBlank()) {
        EditorTextLabel(
            text = "$title: ${"%.2f".format(value)}$suffix",
        )
    }
    val colors = SliderDefaults.colors().copy(
        inactiveTrackColor = MaterialTheme.colorScheme.primary,
    )
    SideEffect {
        if (valueRange == null && add.value != 0f) {
            onValueChanged(value + add.value.toDifference())
        }
    }
    Slider(
        modifier = Modifier.height(16.dp),
        value = if (valueRange == null) add.value else value,
        onValueChange = {
            if (valueRange == null) {
                add.value = it
            } else {
                onValueChanged(it)
            }
        },
        onValueChangeFinished = {
            add.value = 0f
        },
        valueRange = valueRange ?: -5f..5f,
        enabled = enabled,
        interactionSource = interactionSource,
        thumb = {
            Spacer(
                Modifier
                    .size(4.dp, 16.dp)
                    .hoverable(interactionSource = interactionSource)
                    .background(if (enabled) colors.thumbColor else colors.disabledThumbColor, CircleShape)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                modifier = Modifier.height(4.dp),
                colors = colors,
                enabled = enabled,
                sliderState = sliderState,
                trackInsideCornerSize = 0.dp,
                thumbTrackGapSize = 0.dp,
            )
        }
    )
}

private fun Float.toDifference() = when (abs(this)) {
    in 0f..0.5f -> this / 1000
    in 0.5f..1.5f -> this / 100
    in 1.5f..3.5f -> this / 10
    in 3.5f..4.9f -> this
    else -> this * 2
}