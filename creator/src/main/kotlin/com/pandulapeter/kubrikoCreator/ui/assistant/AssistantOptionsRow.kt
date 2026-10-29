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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigSelectOption
import com.agentclientprotocol.model.SessionConfigSelectOptions
import com.pandulapeter.kubrikoCreator.ai.assistant.ClaudeConfiguration
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ai_client_variant
import kubriko.creator.generated.resources.assistant_configuration_default
import kubriko.creator.generated.resources.assistant_configuration_menu
import kubriko.creator.generated.resources.assistant_option_value
import org.jetbrains.compose.resources.stringResource

/**
 * The Claude Code configuration the assistant signs in with, and the options the agent offers for the conversation
 * (its model, mode, or effort), each as a menu.
 */
@Composable
internal fun AssistantOptionsRow(
    configuration: ClaudeConfiguration,
    configurations: List<ClaudeConfiguration>,
    configOptions: List<SessionConfigOption>,
    onConfigurationSelected: (ClaudeConfiguration) -> Unit,
    onConfigOptionSelected: (SessionConfigOption.Select, SessionConfigSelectOption) -> Unit,
    modifier: Modifier = Modifier,
) = FlowRow(
    modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    if (configurations.size > 1 || configuration != ClaudeConfiguration.default) {
        WithTooltip(stringResource(Res.string.assistant_configuration_menu)) {
            MenuButton(
                label = configuration.title(),
                options = configurations,
                optionText = { it.title() },
                onSelected = onConfigurationSelected,
            )
        }
    }
    configOptions.filterIsInstance<SessionConfigOption.Select>().forEach { option ->
        val choices = choicesOf(option)
        WithTooltip(option.description ?: option.name) {
            MenuButton(
                label = choices.firstOrNull { it.value == option.currentValue }?.let { stringResource(Res.string.assistant_option_value, option.name, it.name) } ?: option.name,
                options = choices,
                optionText = { it.name },
                onSelected = { onConfigOptionSelected(option, it) },
            )
        }
    }
}

@Composable
private fun ClaudeConfiguration.title() = variant?.let { stringResource(Res.string.ai_client_variant, stringResource(Res.string.assistant_configuration_default), it) }
    ?: stringResource(Res.string.assistant_configuration_default)

/**
 * Every value [option] can take, its groups flattened.
 */
internal fun choicesOf(option: SessionConfigOption.Select) = when (val options = option.options) {
    is SessionConfigSelectOptions.Flat -> options.options
    is SessionConfigSelectOptions.Grouped -> options.groups.flatMap { it.options }
}
