/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigSelectOption
import com.agentclientprotocol.model.SessionConfigSelectOptions

/**
 * Every choice of this option, its groups' in order when the agent groups them.
 */
public fun SessionConfigOption.Select.choices(): List<SessionConfigSelectOption> = when (val options = options) {
    is SessionConfigSelectOptions.Flat -> options.options
    is SessionConfigSelectOptions.Grouped -> options.groups.flatMap { it.options }
}
