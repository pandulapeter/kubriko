/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.traits

import androidx.compose.ui.geometry.Rect
import com.pandulapeter.kubriko.actor.Actor

interface InsetPaddingAware : Actor {

    fun onInsetPaddingChanged(insetPadding: Rect)
}