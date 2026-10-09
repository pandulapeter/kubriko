/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:JvmName("SmallSliderKt")
@file:JvmMultifileClass

package com.pandulapeter.kubriko.uiComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * A [SmallSlider] with a label displayed next to it.
 *
 * @param modifier The modifier to apply to the row container.
 * @param title The label text to display.
 * @param value The current value of the slider.
 * @param onValueChanged Callback to be invoked when the value changes.
 * @param valueRange The range of values the slider can represent.
 */
@Composable
fun SmallSliderWithTitle(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(
        modifier = Modifier.defaultMinSize(minWidth = 42.dp),
        style = MaterialTheme.typography.labelSmall,
        text = title,
    )
    SmallSlider(
        modifier = Modifier.weight(1f),
        value = value,
        onValueChanged = onValueChanged,
        valueRange = valueRange,
    )
}