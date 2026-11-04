/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

/**
 * Writes several existing Scenes as [Input.contents] has them, all or none: what deleting an Actor type does to the
 * Scenes placing it, and what undoes that byte for byte, each Actor back with its ID in its place. Kubriko Creator's own
 * building block; not a tool.
 */
@OptIn(EditorMutation::class)
internal object ReplaceScenes : WriteCommand<ReplaceScenes.Input> {
    override val name = "replace_scenes"
    override val description = ""
    override val inputSerializer: KSerializer<Input> = NotATool("ReplaceScenes")

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.contents.keys.first())

    override fun targets(context: CommandContext, input: Input) = input.contents.keys.mapTo(LinkedHashSet()) { CommandTarget.Scene(it) }

    /**
     * Checks only that each Scene is there: one with an Actor whose state can't be read is written all the same, keeping
     * that Actor as it is.
     */
    override fun validate(context: CommandContext, input: Input) = input.contents.keys.firstOrNull { !context.scenes.exists(it) }?.let { Rejection(CommandProblem.SCENE_MISSING, it) }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val previous = withContext(context.ioDispatcher) {
            input.contents.mapValues { (scene, _) -> checkNotNull(context.scenes.read(scene)) }.also { context.scenes.writeAllTexts(input.contents) }
        }
        return Applied(inverse = Invocation(ReplaceScenes, Input(previous)))
    }

    /**
     * @param contents The whole text of each Scene, by its name.
     */
    data class Input(
        val contents: Map<String, String>,
    ) {
        init {
            require(contents.isNotEmpty()) { "Replacing Scenes needs at least one." }
        }
    }
}
