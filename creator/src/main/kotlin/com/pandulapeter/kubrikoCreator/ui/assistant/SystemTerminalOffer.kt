/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import com.pandulapeter.kubrikoCreator.ai.assistant.SignInMethod

/**
 * Continuing a sign-in in the system's terminal, where this computer has one.
 *
 * @param isOpening Whether the terminal is being opened, during which it can't be opened again.
 * @param hasFailed Whether it couldn't be opened the last time.
 * @param onContinue Runs a terminal method's command there.
 */
internal data class SystemTerminalOffer(
    val isOpening: Boolean,
    val hasFailed: Boolean,
    val onContinue: (SignInMethod) -> Unit,
)
