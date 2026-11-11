/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentConfiguration
import com.pandulapeter.kubrikoCreator.ai.assistant.RegistryAgent
import com.pandulapeter.kubrikoCreator.ui.assistant.AgentMenu
import com.pandulapeter.kubrikoCreator.ui.assistant.ConfigurationMenu
import com.pandulapeter.kubrikoCreator.ui.assistant.isConfigurationChoice
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_setup_agent
import kubriko.creator.generated.resources.assistant_setup_configuration
import kubriko.creator.generated.resources.settings_assistant_agent_description
import kubriko.creator.generated.resources.settings_assistant_configuration_description
import org.jetbrains.compose.resources.stringResource

/**
 * The assistant's defaults that aren't about one conversation: the agent it runs and, for an agent whose tool Kubriko
 * Creator knows, the configuration it runs as (each agent remembers its own). A custom agent is entered in the panel,
 * whose form tests its command.
 */
@Composable
internal fun AssistantSettingsPage(
    agent: AgentChoice,
    agents: List<RegistryAgent>,
    customAgentCommand: String?,
    changingFilesUnasked: Set<String>,
    configuration: AgentConfiguration?,
    configurations: List<AgentConfiguration>,
    onAgentSelected: (AgentChoice) -> Unit,
    onConfigurationSelected: (AgentConfiguration) -> Unit,
    onOpenLog: (() -> Unit)?,
) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    SettingsRow(
        label = stringResource(Res.string.assistant_setup_agent),
        description = stringResource(Res.string.settings_assistant_agent_description),
    ) {
        Column(Modifier.widthIn(max = CONTROL_WIDTH)) {
            AgentMenu(
                agent = agent,
                agents = agents,
                customAgentCommand = customAgentCommand,
                changingFilesUnasked = changingFilesUnasked,
                onSelected = onAgentSelected,
                onEnterCustomAgent = null,
                onOpenLog = onOpenLog,
            )
        }
    }
    if (configuration != null && isConfigurationChoice(configuration, configurations)) {
        SettingsRow(
            label = stringResource(Res.string.assistant_setup_configuration),
            description = stringResource(Res.string.settings_assistant_configuration_description, configuration.client.displayName),
        ) {
            Column(Modifier.widthIn(max = CONTROL_WIDTH)) {
                ConfigurationMenu(
                    configuration = configuration,
                    configurations = configurations,
                    onSelected = onConfigurationSelected,
                )
            }
        }
    }
}

private val CONTROL_WIDTH = 280.dp
