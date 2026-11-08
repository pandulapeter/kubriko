/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.git

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.git.ChangeEntry
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.ui.LocalDescriptionWriter
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_update_screenplay_with_ai
import kubriko.creator.generated.resources.assistant_prompt_update_screenplay
import kubriko.creator.generated.resources.git_screenplay_reminder
import org.jetbrains.compose.resources.stringResource

/**
 * Says, above the commit form, that the game changed but no page of the screenplay did, with the assistant one click away
 * to bring the pages up to date. It never stands in the way of the commit.
 */
@Composable
internal fun ScreenplayReminder(
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.padding(horizontal = 12.dp),
) {
    Text(
        text = stringResource(Res.string.git_screenplay_reminder),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val writer = LocalDescriptionWriter.current
    if (writer != null && LocalIsAiEnabled.current) {
        val prompt = stringResource(Res.string.assistant_prompt_update_screenplay)
        TextButton(
            enabled = !writer.isBusy,
            onClick = { writer.write(prompt) },
        ) {
            Text(stringResource(Res.string.action_update_screenplay_with_ai), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * Whether [changes] touch the game (its types, Scenes, resources, or Kotlin) but no page of the screenplay.
 */
internal fun isScreenplayReminderDue(changes: List<ChangeEntry>): Boolean {
    val paths = changes.flatMap { it.paths }
    val touchesGame = paths.any { path ->
        path.startsWith(".kubriko/actors/") || path.startsWith(".kubriko/managers/") || "composeResources/" in path || (path.endsWith(".kt") && "src/" in path)
    }
    return touchesGame && paths.none { PageNaming.nameOf(it) != null }
}
