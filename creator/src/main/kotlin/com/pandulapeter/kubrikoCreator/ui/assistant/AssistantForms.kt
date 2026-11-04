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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Which of the assistant's forms is open: shared by the panel, which shows them, and the setup in the AI panel's header,
 * which opens them.
 */
@Stable
internal class AssistantForms {
    /**
     * Whether the API key is being entered.
     */
    var isEnteringApiKey by mutableStateOf(false)

    /**
     * Whether the command of a custom agent is being entered.
     */
    var isEnteringCustomAgent by mutableStateOf(false)
}

/**
 * Remembers [AssistantForms] with none of them open.
 */
@Composable
internal fun rememberAssistantForms(): AssistantForms = remember { AssistantForms() }
