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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource

/**
 * Why what was typed into the field above can't be used ([error]), small and in the error color.
 */
@Composable
public fun FieldErrorText(
    error: FieldError,
    modifier: Modifier = Modifier,
): Unit = Text(
    modifier = modifier,
    text = stringResource(error.text, *error.arguments.toTypedArray()),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.error,
)
