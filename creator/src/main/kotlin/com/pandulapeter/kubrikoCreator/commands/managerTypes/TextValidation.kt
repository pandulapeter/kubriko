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
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireManagerType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_text_missing

internal fun CommandContext.validateTextIndex(managerType: String, index: Int): Rejection? = validateManagerType(managerType)
    ?: if (index !in requireManagerType(managerType).texts.indices) Rejection(Res.string.error_command_text_missing, managerType, index.toString()) else null
