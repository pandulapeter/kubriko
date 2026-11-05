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
 * A change to one of the user's documents waiting for the user to confirm it ([DocumentChangeConfirmation]): deleting it,
 * or moving it into the screenplay. It compares by identity, so asking again for the same change asks again.
 *
 * @param onSuccess What runs once the change was applied, never when it was rejected.
 */
internal class PendingDocumentChange(
    val invocation: Invocation<*>,
    val onSuccess: () -> Unit = {},
)
