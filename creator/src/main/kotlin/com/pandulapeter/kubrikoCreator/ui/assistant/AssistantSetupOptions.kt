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
import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigSelectOption
import com.agentclientprotocol.model.SessionConfigSelectOptions
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentConfiguration
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ai_client_variant
import kubriko.creator.generated.resources.assistant_configuration_menu
import kubriko.creator.generated.resources.assistant_setup_configuration
import org.jetbrains.compose.resources.stringResource

/**
 * The assistant's setup beyond its agent and account: which of its tool's configurations it runs as (when there is a choice), and
 * the options the agent's session offers, such as its model, one row each.
 */
@Composable
internal fun AssistantSetupOptions(
    configuration: AgentConfiguration?,
    configurations: List<AgentConfiguration>,
    configOptions: List<SessionConfigOption>,
    onConfigurationSelected: (AgentConfiguration) -> Unit,
    onConfigOptionSelected: (SessionConfigOption.Select, SessionConfigSelectOption) -> Unit,
) {
    if (configuration != null && (configurations.size > 1 || configuration.alias != null)) {
        AssistantSetupRow(stringResource(Res.string.assistant_setup_configuration)) {
            WithTooltip(stringResource(Res.string.assistant_configuration_menu, configuration.client.displayName)) {
                MenuButton(
                    label = configuration.title(),
                    options = configurations,
                    optionText = { it.title() },
                    onSelected = onConfigurationSelected,
                    isWrapping = true,
                )
            }
        }
    }
    configOptions.filterIsInstance<SessionConfigOption.Select>().forEach { option ->
        val choices = choicesOf(option)
        AssistantSetupRow(option.name) {
            WithTooltip(option.description ?: option.name) {
                MenuButton(
                    label = choices.firstOrNull { it.value == option.currentValue }?.name ?: option.name,
                    options = choices,
                    optionText = { it.name },
                    onSelected = { onConfigOptionSelected(option, it) },
                    isWrapping = true,
                )
            }
        }
    }
}

/**
 * What the session's options are set to, by the names of their current choices, for summing the setup up in a line.
 */
internal fun currentChoices(configOptions: List<SessionConfigOption>): List<String> = configOptions.filterIsInstance<SessionConfigOption.Select>().mapNotNull { option ->
    choicesOf(option).firstOrNull { it.value == option.currentValue }?.name
}

@Composable
private fun AgentConfiguration.title() = variant?.let { stringResource(Res.string.ai_client_variant, client.displayName, it) } ?: client.displayName

internal fun choicesOf(option: SessionConfigOption.Select) = when (val options = option.options) {
    is SessionConfigSelectOptions.Flat -> options.options
    is SessionConfigSelectOptions.Grouped -> options.groups.flatMap { it.options }
}
