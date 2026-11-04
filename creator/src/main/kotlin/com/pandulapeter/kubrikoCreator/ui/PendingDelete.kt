/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.commands.Invocation

/**
 * A delete waiting for the user to confirm it once they have seen what it changes ([DeleteConfirmation]). It compares by
 * identity, so asking again for the same delete reads its usages again.
 *
 * @param onSuccess What runs once the delete was applied, never when it was rejected.
 */
internal class PendingDelete(
    val invocation: Invocation<*>,
    val onSuccess: () -> Unit = {},
)
