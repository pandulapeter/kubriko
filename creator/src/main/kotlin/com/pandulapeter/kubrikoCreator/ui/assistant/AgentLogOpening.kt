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

import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantPanelModel
import com.pandulapeter.kubrikoCreator.ui.components.openInSystemEditor

/**
 * Opens the agents' log in the system's editor once there is one, or null where agents keep none.
 */
internal fun AssistantPanelModel.agentLogOpening(): (() -> Unit)? = errorLogFile?.let { file -> { if (file.isFile) openInSystemEditor(file) } }
