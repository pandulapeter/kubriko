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

import org.jetbrains.compose.resources.StringResource

/**
 * Why a command wasn't applied, shown to the user and returned to the AI.
 *
 * @param arguments The values of the [reason]'s placeholders.
 */
internal data class Rejection(
    val reason: StringResource,
    val arguments: List<String> = emptyList(),
) {
    constructor(reason: StringResource, vararg arguments: String) : this(reason, arguments.toList())
}
