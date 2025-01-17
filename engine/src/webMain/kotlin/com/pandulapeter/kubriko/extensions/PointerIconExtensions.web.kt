/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.extensions

import androidx.compose.ui.input.pointer.PointerIcon

// Can't implement invisible cursor because DarwinCursor is private to Compose
internal actual val pointerIconInvisible: PointerIcon = PointerIcon.Crosshair