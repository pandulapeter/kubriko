/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * The result of a [WriteCommand] that was applied.
 *
 * @param inverse The command that reverts the change, or null if it can't be undone (a git commit, for example).
 * @param result What the change produced, for the AI (the index of a placed Actor, for example).
 */
internal data class Applied(
    val inverse: Invocation<*>?,
    val result: JsonElement = JsonNull,
)
