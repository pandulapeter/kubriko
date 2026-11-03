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

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

/**
 * One entry of a [RightClickMenu]: its [label] and [icon], drawn in the error color when [isDestructive].
 */
@Immutable
public class ContextAction(
    public val label: String,
    public val icon: DrawableResource? = null,
    public val isDestructive: Boolean = false,
    public val onClick: () -> Unit,
)
