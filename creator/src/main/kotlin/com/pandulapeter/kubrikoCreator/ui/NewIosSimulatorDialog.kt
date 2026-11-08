/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import com.pandulapeter.kubrikoCreator.ui.components.FailureDetails
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.SearchableChoiceButton
import com.pandulapeter.kubrikoCreator.ui.components.fadeInOnChange
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import com.pandulapeter.kubrikoCreator.workspace.IosSimulatorCreation
import com.pandulapeter.kubrikoCreator.workspace.IosSimulatorOptions
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_check_again
import kubriko.creator.generated.resources.action_close
import kubriko.creator.generated.resources.action_create_simulator
import kubriko.creator.generated.resources.new_ios_simulator_creating
import kubriko.creator.generated.resources.new_ios_simulator_description
import kubriko.creator.generated.resources.new_ios_simulator_device
import kubriko.creator.generated.resources.new_ios_simulator_name
import kubriko.creator.generated.resources.new_ios_simulator_no_runtime
import kubriko.creator.generated.resources.new_ios_simulator_reading
import kubriko.creator.generated.resources.new_ios_simulator_refused
import kubriko.creator.generated.resources.new_ios_simulator_timed_out
import kubriko.creator.generated.resources.new_ios_simulator_title
import kubriko.creator.generated.resources.new_ios_simulator_unreadable
import kubriko.creator.generated.resources.new_ios_simulator_version
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Makes a new iOS simulator from a name, a device type, and an iOS version Xcode offers ([state]), which Play then uses.
 * It can't be closed while the simulator is being made, since closing would stop `simctl` halfway.
 */
@Composable
internal fun NewIosSimulatorDialog(
    state: NewIosSimulatorState,
    onDismiss: () -> Unit,
) = CreatorDialog(
    title = stringResource(Res.string.new_ios_simulator_title),
    maxWidth = 560.dp,
    onDismiss = { if (!state.isCreating) onDismiss() },
    confirmButton = {
        when (state.options) {
            is IosSimulatorOptions.Ready -> Button(onClick = state::create, enabled = state.canCreate) {
                Text(stringResource(if (state.isCreating) Res.string.new_ios_simulator_creating else Res.string.action_create_simulator))
            }
            IosSimulatorOptions.NoRuntime -> Button(onClick = state::reload) { Text(stringResource(Res.string.action_check_again)) }
            is IosSimulatorOptions.Unreadable, null -> TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) }
        }
    },
    dismissButton = when (state.options) {
        is IosSimulatorOptions.Ready, IosSimulatorOptions.NoRuntime -> {
            { TextButton(onClick = onDismiss, enabled = !state.isCreating) { Text(stringResource(Res.string.action_cancel)) } }
        }
        is IosSimulatorOptions.Unreadable, null -> null
    },
    text = {
        Column(
            modifier = Modifier.width(DIALOG_CONTENT_WIDTH).animateContentSize(CreatorMotion.medium()).fadeInOnChange(state.options?.let { it::class }),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val options = state.options) {
                null -> QuietText(Res.string.new_ios_simulator_reading)
                IosSimulatorOptions.NoRuntime -> Text(stringResource(Res.string.new_ios_simulator_no_runtime))
                is IosSimulatorOptions.Unreadable -> {
                    Text(stringResource(Res.string.new_ios_simulator_unreadable))
                    options.reason?.let { FailureDetails(it) }
                }
                is IosSimulatorOptions.Ready -> NewIosSimulatorFields(state, options)
            }
        }
    },
)

@Composable
private fun NewIosSimulatorFields(
    state: NewIosSimulatorState,
    options: IosSimulatorOptions.Ready,
) {
    QuietText(Res.string.new_ios_simulator_description)
    FieldRow(Res.string.new_ios_simulator_name) {
        CompactTextField(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CONTROL_HEIGHT)
                .onPreviewKeyEvent { event -> (event.type == KeyEventType.KeyDown && event.key == Key.Enter).also { if (it) state.create() } },
            value = state.name,
            onValueChange = state::changeName,
            isError = state.nameError != null,
        )
        state.nameError?.let { error ->
            Text(
                text = stringResource(error.text, *error.arguments.toTypedArray()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    FieldRow(Res.string.new_ios_simulator_device) {
        SearchableChoiceButton(
            label = state.deviceType?.name.orEmpty(),
            options = options.deviceTypes,
            optionText = { it.name },
            onSelected = state::chooseDeviceType,
        )
    }
    FieldRow(Res.string.new_ios_simulator_version) {
        val runtimes = state.runtimes
        MenuButton(
            label = state.runtime?.labelAmong(runtimes).orEmpty(),
            options = runtimes,
            optionText = { it.labelAmong(runtimes) },
            onSelected = state::chooseRuntime,
        )
    }
    state.failure?.let { CreationFailure(it) }
}

/**
 * One field of the dialog: its [label] on the left, lined up with the others, and its [content] beside it.
 */
@Composable
private fun FieldRow(
    label: StringResource,
    content: @Composable () -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.Top,
) {
    Box(
        modifier = Modifier.width(LABEL_WIDTH).heightIn(min = CONTROL_HEIGHT),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        content()
    }
}

@Composable
private fun CreationFailure(failure: IosSimulatorCreation) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        text = stringResource(if (failure is IosSimulatorCreation.TimedOut) Res.string.new_ios_simulator_timed_out else Res.string.new_ios_simulator_refused),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
    (failure as? IosSimulatorCreation.Refused)?.let { FailureDetails(it.reason) }
}

@Composable
private fun QuietText(text: StringResource) = Text(
    text = stringResource(text),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

private val DIALOG_CONTENT_WIDTH = 460.dp
private val LABEL_WIDTH = 96.dp

/**
 * As tall as a menu's button, so every field's label lines up with its control.
 */
private val CONTROL_HEIGHT = 32.dp
