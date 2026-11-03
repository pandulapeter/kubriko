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

import org.jetbrains.compose.resources.StringResource

/**
 * Why what was typed into a field can't be used.
 *
 * @param arguments The values of the [text]'s placeholders.
 */
internal data class FieldError(
    val text: StringResource,
    val arguments: List<String> = emptyList(),
)
