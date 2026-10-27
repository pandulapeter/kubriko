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

import com.pandulapeter.kubriko.creator.CreatorEntryPoint

/**
 * A project's [CreatorEntryPoint], loaded into Kubriko Creator's own process. [close] releases the project's classes.
 */
internal class LoadedEntryPoint(
    val entryPoint: CreatorEntryPoint,
    val close: () -> Unit,
)
