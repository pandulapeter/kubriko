/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import kotlinx.serialization.Serializable

/**
 * What the assistant panel remembers between runs of Kubriko Creator.
 *
 * @param claudeConfigurationAlias The shell alias whose Claude Code configuration Claude Agent uses, or null for the default one.
 */
@Serializable
internal data class AssistantSettings(
    val claudeConfigurationAlias: String? = null,
)
