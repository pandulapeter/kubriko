/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * The types whose generated classes share the Timers of [typeName], among [types]: every Actor type of its family
 * (whose topmost type's class declares them), or a Manager type alone.
 */
public fun timerSharersOf(
    typeName: String,
    types: List<BlockTypeDefinition>,
    family: ActorTypeFamily = ActorTypeFamily(types.filterIsInstance<ActorTypeDefinition>()),
): List<String> = if (types.firstOrNull { it.name == typeName } is ActorTypeDefinition) family.familyOf(family.rootOf(typeName)) else listOf(typeName)
