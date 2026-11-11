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
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentConfiguration
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ai_client_variant
import kubriko.creator.generated.resources.assistant_configuration_menu
import org.jetbrains.compose.resources.stringResource

/**
 * Which of the agent's tool's configurations it runs as, as a menu of the ones found on this computer.
 */
@Composable
internal fun ConfigurationMenu(
    configuration: AgentConfiguration,
    configurations: List<AgentConfiguration>,
    onSelected: (AgentConfiguration) -> Unit,
) = WithTooltip(stringResource(Res.string.assistant_configuration_menu, configuration.client.displayName)) {
    MenuButton(
        label = configuration.title(),
        options = configurations,
        optionText = { it.title() },
        onSelected = onSelected,
        isWrapping = true,
    )
}

/**
 * Whether there is a configuration to choose: more than one was found, or the one used is an alias's.
 */
internal fun isConfigurationChoice(configuration: AgentConfiguration, configurations: List<AgentConfiguration>) =
    configurations.size > 1 || configuration.alias != null

@Composable
private fun AgentConfiguration.title() = variant?.let { stringResource(Res.string.ai_client_variant, client.displayName, it) } ?: client.displayName
