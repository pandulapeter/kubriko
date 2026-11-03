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
 * One entry of a [RightClickMenu]: its [label], with an optional [description] under it, and its [icon], drawn in the
 * error color when [isDestructive]. One that [isChecked] (true or false) is a choice that stays on or off, shown by a
 * check mark in place of the icon.
 */
@Immutable
public class ContextAction(
    public val label: String,
    public val icon: DrawableResource? = null,
    public val isDestructive: Boolean = false,
    public val description: String? = null,
    public val isChecked: Boolean? = null,
    public val onClick: () -> Unit,
)
