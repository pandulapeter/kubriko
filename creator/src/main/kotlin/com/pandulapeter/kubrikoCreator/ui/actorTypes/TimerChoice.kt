/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.foundation.DomainText
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.NewNameField
import com.pandulapeter.kubrikoCreator.ui.toFieldError
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add
import kubriko.creator.generated.resources.action_rename
import kubriko.creator.generated.resources.timer_delete
import kubriko.creator.generated.resources.timer_name_placeholder
import kubriko.creator.generated.resources.timer_new
import kubriko.creator.generated.resources.timer_rename
import org.jetbrains.compose.resources.stringResource

/**
 * Which Timer an Event or a Block is about: one of [timerNames] (the Timers the type and its family use), or a new
 * one, named in place. With [onRename], the menu also renames [timer] wherever it is used, and with [onDelete] deletes
 * it with everything using it, once that is confirmed.
 */
@Composable
internal fun TimerChoice(
    timer: String,
    timerNames: List<String>,
    onSelected: (String) -> Unit,
    onRename: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var naming by remember { mutableStateOf<TimerOption?>(null) }
    when (val option = naming) {
        null -> MenuButton(
            label = timer,
            options = timerOptions(timer, timerNames, canRename = onRename != null, canDelete = onDelete != null),
            optionText = { it.text() },
            onSelected = { selected ->
                when (selected) {
                    is TimerOption.Existing -> onSelected(selected.name)
                    TimerOption.Delete -> onDelete?.invoke()
                    TimerOption.New, TimerOption.Rename -> naming = selected
                }
            },
        )
        else -> Box(
            modifier = Modifier.width(280.dp),
        ) {
            val isRenaming = option == TimerOption.Rename
            NewNameField(
                placeholder = Res.string.timer_name_placeholder,
                initialName = if (isRenaming) timer else "",
                indent = 0.dp,
                confirmLabel = if (isRenaming) Res.string.action_rename else Res.string.action_add,
                onCancel = { naming = null },
                onCreate = { name ->
                    timerNameError(name, timer, timerNames, isRenaming).also { error ->
                        if (error == null) {
                            naming = null
                            when {
                                !isRenaming -> onSelected(name)
                                name != timer -> onRename?.invoke(name)
                            }
                        }
                    }?.toFieldError()
                },
            )
        }
    }
}

/**
 * What the Timer menu offers: the Timers in use (with [timer] among them, even if nothing else uses it), naming a new
 * one, renaming [timer] if [canRename], and deleting it if [canDelete].
 */
internal fun timerOptions(timer: String, timerNames: List<String>, canRename: Boolean, canDelete: Boolean = false) =
    (timerNames + timer).distinct().sorted().map(TimerOption::Existing) +
        listOfNotNull(TimerOption.New, TimerOption.Rename.takeIf { canRename }, TimerOption.Delete.takeIf { canDelete })

/**
 * Why [name] can't be the name of a new Timer, or of [timer] renamed, among [timerNames], or null if it can. Naming a
 * new Timer like one in use only chooses that one.
 */
internal fun timerNameError(name: String, timer: String, timerNames: List<String>, isRenaming: Boolean): DomainText? = TimerNaming.validate(name) ?: when {
    isRenaming && name != timer && name in timerNames -> CommandProblem.TIMER_TAKEN
    else -> null
}

internal sealed interface TimerOption {

    data class Existing(val name: String) : TimerOption

    data object New : TimerOption

    data object Rename : TimerOption

    data object Delete : TimerOption
}

@Composable
private fun TimerOption.text() = when (this) {
    is TimerOption.Existing -> name
    TimerOption.New -> stringResource(Res.string.timer_new)
    TimerOption.Rename -> stringResource(Res.string.timer_rename)
    TimerOption.Delete -> stringResource(Res.string.timer_delete)
}
