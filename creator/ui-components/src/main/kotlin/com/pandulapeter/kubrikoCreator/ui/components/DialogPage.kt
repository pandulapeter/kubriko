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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

/**
 * One page of a [MasterDetailDialog]: listed by its [icon] and [title], shown with its [description] above its [content].
 * [key] tells pages apart while their titles change.
 */
@Immutable
public class DialogPage(
    public val key: String,
    public val icon: DrawableResource,
    public val title: String,
    public val description: String? = null,
    public val content: @Composable () -> Unit,
)
