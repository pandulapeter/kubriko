/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.timerNames
import com.pandulapeter.kubrikoCreator.actors.timerSharersOf
import com.pandulapeter.kubrikoCreator.commands.CommandContext

/**
 * The types sharing the Timers of [typeName] ([timerSharersOf]).
 */
internal fun CommandContext.timerSharersOf(typeName: String): List<String> =
    timerSharersOf(typeName, requireBlockTypes.types.value, ActorTypeFamily(requireBlockTypes.actorTypes.value))

/**
 * The Timers [typeName] uses, with those of every type sharing them.
 */
internal fun CommandContext.timerNamesOf(typeName: String): List<String> = timerSharersOf(typeName).flatMap { requireType(it).timerNames }.distinct()
