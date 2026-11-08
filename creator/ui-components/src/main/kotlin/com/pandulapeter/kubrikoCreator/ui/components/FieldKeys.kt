/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Makes Enter in this field confirm what was typed ([onConfirm]) and Escape give it up ([onCancel]), before the field
 * itself sees either key.
 */
public fun Modifier.confirmingOnEnter(onConfirm: () -> Unit, onCancel: () -> Unit): Modifier = onPreviewKeyEvent { event ->
    when {
        event.type != KeyEventType.KeyDown -> false
        event.key == Key.Enter -> true.also { onConfirm() }
        event.key == Key.Escape -> true.also { onCancel() }
        else -> false
    }
}
