/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Reports the focus state of the editor's text inputs, so global keyboard shortcuts (such as the
 * camera movement keys) can be suppressed while the user is typing.
 */
internal val LocalTextInputFocusReporter = staticCompositionLocalOf<(Boolean) -> Unit> { {} }
