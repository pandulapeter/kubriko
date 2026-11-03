/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.GameTargets
import com.pandulapeter.kubrikoCreator.template.LineDiff
import com.pandulapeter.kubrikoCreator.template.TargetChange
import com.pandulapeter.kubrikoCreator.template.TargetProblem
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * The game's targets, or why they can't be changed in this project.
 */
internal fun CommandContext.targetsOrRejection(target: GameTarget): Pair<GameTargets?, Rejection?> = when {
    target == GameTarget.DESKTOP -> null to Rejection(TargetProblem.DESKTOP_ALWAYS_ON)
    else -> buildFiles.targets?.let { it to null } ?: (null to Rejection(TargetProblem.CHANGED_BY_HAND, target.key))
}

/**
 * [change] as the AI reads it: its ID, whether undo can turn it back, each changed file as the lines that change (with two
 * around them), and the files created.
 */
internal fun changeJson(change: TargetChange.Proposed): JsonObject = buildJsonObject {
    put("changeId", change.id)
    put("canBeUndone", change.isReversible)
    putJsonArray("changes") {
        change.changes.filterNot { it.isNew }.forEach { file ->
            add(
                buildJsonObject {
                    put("path", file.path)
                    put(
                        "diff",
                        LineDiff.withContext(LineDiff.of(file.before, file.after.orEmpty())).joinToString("\n") { line ->
                            line?.let { it.kind.prefix + it.text } ?: "…"
                        },
                    )
                },
            )
        }
    }
    putJsonArray("created") { change.changes.filter { it.isNew }.forEach { add(it.path) } }
}
