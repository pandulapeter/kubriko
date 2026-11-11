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
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.RegistryAgent
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_open_agent_log
import kubriko.creator.generated.resources.assistant_agent_custom
import kubriko.creator.generated.resources.assistant_agent_menu
import kubriko.creator.generated.resources.assistant_agent_unasked
import kubriko.creator.generated.resources.assistant_agent_untested
import kubriko.creator.generated.resources.assistant_agent_untested_unasked
import org.jetbrains.compose.resources.stringResource

/**
 * The agent the assistant runs, as a menu of the registry's agents, the custom one if the user entered it, entering a
 * custom agent (where [onEnterCustomAgent] can show its form), and opening the agents' log, when there is one. Untested
 * agents, and those seen changing files without asking ([changingFilesUnasked], by [AgentChoice.identity]), are marked as
 * such.
 */
@Composable
internal fun AgentMenu(
    agent: AgentChoice,
    agents: List<RegistryAgent>,
    customAgentCommand: String?,
    changingFilesUnasked: Set<String>,
    onSelected: (AgentChoice) -> Unit,
    onEnterCustomAgent: (() -> Unit)?,
    onOpenLog: (() -> Unit)? = null,
) = WithTooltip(stringResource(Res.string.assistant_agent_menu)) {
    MenuButton(
        label = agent.label(changingFilesUnasked),
        options = agentMenuItems(agents, customAgentCommand, canEnterCustom = onEnterCustomAgent != null, hasLog = onOpenLog != null),
        optionText = { item ->
            when (item) {
                is AgentMenuItem.Agent -> item.choice.label(changingFilesUnasked)
                AgentMenuItem.EnterCustom -> stringResource(Res.string.assistant_agent_custom)
                AgentMenuItem.OpenLog -> stringResource(Res.string.action_open_agent_log)
            }
        },
        onSelected = { item ->
            when (item) {
                is AgentMenuItem.Agent -> onSelected(item.choice)
                AgentMenuItem.EnterCustom -> onEnterCustomAgent?.invoke()
                AgentMenuItem.OpenLog -> onOpenLog?.invoke()
            }
        },
        isWrapping = true,
    )
}

internal sealed interface AgentMenuItem {
    data class Agent(
        val choice: AgentChoice,
    ) : AgentMenuItem

    data object EnterCustom : AgentMenuItem

    data object OpenLog : AgentMenuItem
}

/**
 * What the agent menu offers: the registry's agents in their order, the custom agent once there is one, entering one where
 * that can be done, and the agents' log where there is one.
 */
internal fun agentMenuItems(
    agents: List<RegistryAgent>,
    customAgentCommand: String?,
    canEnterCustom: Boolean = true,
    hasLog: Boolean = false,
): List<AgentMenuItem> =
    agents.map { AgentMenuItem.Agent(AgentChoice.Registry(it)) } +
        listOfNotNull(
            customAgentCommand?.let { AgentMenuItem.Agent(AgentChoice.Custom(it)) },
            AgentMenuItem.EnterCustom.takeIf { canEnterCustom },
            AgentMenuItem.OpenLog.takeIf { hasLog },
        )

@Composable
private fun AgentChoice.label(changingFilesUnasked: Set<String>) = when {
    identity in changingFilesUnasked -> stringResource(if (isTested) Res.string.assistant_agent_unasked else Res.string.assistant_agent_untested_unasked, name)
    isTested -> name
    else -> stringResource(Res.string.assistant_agent_untested, name)
}
