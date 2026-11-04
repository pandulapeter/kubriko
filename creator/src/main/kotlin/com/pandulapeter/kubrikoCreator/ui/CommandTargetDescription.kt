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

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ai_target_block
import kubriko.creator.generated.resources.ai_target_event
import kubriko.creator.generated.resources.ai_target_game
import kubriko.creator.generated.resources.ai_target_page
import kubriko.creator.generated.resources.ai_target_project
import kubriko.creator.generated.resources.ai_target_scene
import kubriko.creator.generated.resources.ai_target_version_control
import org.jetbrains.compose.resources.stringResource

/**
 * What a step works on, in words: `Paddle, Event 2, Block 1`, counting from one.
 */
@Composable
internal fun CommandTarget.description(): String = when (this) {
    is CommandTarget.BlockType -> {
        val eventIndex = eventIndex
        val blockIndex = blockIndex
        when {
            eventIndex == null -> name
            blockIndex == null -> stringResource(Res.string.ai_target_event, name, (eventIndex + 1).toString())
            else -> stringResource(Res.string.ai_target_block, name, (eventIndex + 1).toString(), (blockIndex + 1).toString())
        }
    }
    is CommandTarget.Scene -> stringResource(Res.string.ai_target_scene, name)
    is CommandTarget.Page -> stringResource(Res.string.ai_target_page, name)
    CommandTarget.VersionControl -> stringResource(Res.string.ai_target_version_control)
    CommandTarget.Game -> stringResource(Res.string.ai_target_game)
    CommandTarget.Project -> stringResource(Res.string.ai_target_project)
}
