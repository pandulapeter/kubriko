/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

/**
 * What a sidebar row holding folders offers for all of them at once: opening every one, and closing every one.
 */
internal class SubfolderActions(
    val onExpandAll: () -> Unit,
    val onCollapseAll: () -> Unit,
)
