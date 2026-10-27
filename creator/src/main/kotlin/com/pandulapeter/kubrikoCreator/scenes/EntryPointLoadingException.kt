/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import org.jetbrains.compose.resources.StringResource

/**
 * A project whose entry point couldn't be loaded, with a reason to show the user.
 */
internal class EntryPointLoadingException(
    val reason: StringResource,
    cause: Throwable? = null,
) : Exception(cause)
