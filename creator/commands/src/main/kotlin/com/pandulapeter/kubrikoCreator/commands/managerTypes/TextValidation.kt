/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.managerTypes

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireManagerType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType

internal fun CommandContext.validateTextIndex(managerType: String, index: Int): Rejection? = validateManagerType(managerType) ?: run {
    val texts = requireManagerType(managerType).texts
    when {
        index in texts.indices -> null
        texts.isEmpty() -> Rejection(LookupProblem.TEXT_MISSING_NONE, managerType, index.toString())
        else -> Rejection(LookupProblem.TEXT_MISSING, managerType, index.toString(), texts.lastIndex.toString())
    }
}
