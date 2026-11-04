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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentclientprotocol.annotations.UnstableApi
import com.agentclientprotocol.model.SessionConfigOptionValue
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantPanelModel
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.Popover
import com.pandulapeter.kubrikoCreator.ui.components.openInSystemEditor
import com.pandulapeter.kubrikoCreator.ui.components.rememberMenuAnchor
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_assistant_setup
import kubriko.creator.generated.resources.action_new_conversation
import kubriko.creator.generated.resources.assistant_setup_agent
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_settings
import org.jetbrains.compose.resources.stringResource

/**
 * The assistant's buttons in the AI panel's header: one opening its setup (which agent it runs, as whom, and with what
 * options, which rarely change, so they stay out of sight and are named in the button's tooltip; a choice that goes on in
 * the panel, such as signing in, closes it), and one starting a conversation, unless the one shown has nothing in it yet.
 * They sit there rather than beside the conversation's title, which needs the room.
 */
@Composable
internal fun AssistantHeaderActions(
    model: AssistantPanelModel,
    forms: AssistantForms,
) {
    val agent = model.agent.collectAsState().value
    CompositionLocalProvider(LocalAgent provides agent) {
        val configOptions = model.currentChat.collectAsState().value?.configOptions?.collectAsState()?.value.orEmpty()
        SetupButton(
            summary = (listOf(agent.name) + currentChoices(configOptions)).joinToString(SUMMARY_SEPARATOR),
        ) { close ->
            AssistantSetup(model = model, forms = forms, close = close)
        }
        IconActionButton(
            icon = Res.drawable.ic_add,
            label = Res.string.action_new_conversation,
            onClick = model::newChat,
            isEnabled = model.currentChat.collectAsState().value?.isEmpty?.collectAsState()?.value != true,
        )
    }
}

@Composable
private fun SetupButton(
    summary: String,
    setup: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val menu = rememberRightClickMenuState()
    val anchor = rememberMenuAnchor(menu)
    IconActionButton(
        modifier = anchor.modifier,
        icon = Res.drawable.ic_settings,
        label = stringResource(Res.string.action_assistant_setup, summary),
        onClick = anchor::open,
    )
    Popover(
        state = menu,
        // As wide as its widest row, so that a long account or model name shows in full; in a window too narrow for it, the rows wrap.
        modifier = Modifier.widthIn(min = SETUP_MINIMUM_WIDTH).width(IntrinsicSize.Max),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            setup(menu::close)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AssistantSetup(
    model: AssistantPanelModel,
    forms: AssistantForms,
    close: () -> Unit,
) {
    val chat = model.currentChat.collectAsState().value
    AssistantSetupRow(stringResource(Res.string.assistant_setup_agent)) {
        AgentMenu(
            agent = LocalAgent.current,
            agents = model.agents.collectAsState().value,
            customAgentCommand = model.customAgentCommand,
            onSelected = model::selectAgent,
            onEnterCustomAgent = {
                close()
                forms.isEnteringCustomAgent = true
            },
            onOpenLog = model.errorLogFile?.let { file -> { if (file.isFile) openInSystemEditor(file) } },
        )
    }
    AssistantAccountMenu(
        status = model.authStatus.collectAsState().value,
        methods = model.signInMethods.collectAsState().value,
        hasApiKey = model.hasApiKey.collectAsState().value,
        canKeepApiKey = model.canKeepApiKey,
        onSignIn = { method ->
            close()
            model.signIn(method)
        },
        onUseApiKey = {
            close()
            forms.isEnteringApiKey = true
        },
        onRemoveApiKey = model::removeApiKey,
    )
    AssistantSetupOptions(
        configuration = model.configuration.collectAsState().value,
        configurations = model.configurations.collectAsState().value,
        configOptions = chat?.configOptions?.collectAsState()?.value.orEmpty(),
        onConfigurationSelected = model::selectConfiguration,
        onConfigOptionSelected = { option, choice -> chat?.setConfigOption(option.id, SessionConfigOptionValue.of(choice.value.value)) },
    )
}

private const val SUMMARY_SEPARATOR = " · "
private val SETUP_MINIMUM_WIDTH = 320.dp
