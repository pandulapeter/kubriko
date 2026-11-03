/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.TargetChange

/**
 * Turning [target] on or off, as [isOn] says, waiting for the user to confirm what it takes ([result]).
 */
internal data class PendingTargetChange(
    val target: GameTarget,
    val isOn: Boolean,
    val result: TargetChange,
)
