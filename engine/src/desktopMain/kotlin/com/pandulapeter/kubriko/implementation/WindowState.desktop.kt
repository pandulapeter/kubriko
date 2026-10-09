/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:JvmName("PlatformUtils_desktopKt")

package com.pandulapeter.kubriko.implementation

import androidx.compose.ui.window.WindowState

/**
 * The state of the desktop window showing the game. Assign it from the `application { }` block,
 * `windowState = rememberWindowState(...)`, for `PointerInputManager.tryToMoveHoveringPointer` to work; while it is
 * unset that function returns false.
 */
lateinit var windowState: WindowState
