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

/**
 * One change made to the project through a command, as the AI is told about the changes it didn't make.
 *
 * @param revision The project's revision after it: one more than before it.
 * @param origin Which of the assistant's conversations made it, if one did.
 * @param isByAi Whether an AI client made it, through a tool call.
 * @param step Whether it undid or redid an earlier change, or null for a new one.
 */
public data class ChangeRecord(
    val revision: Int,
    val commandName: String,
    val target: CommandTarget?,
    val origin: AiStepOrigin? = null,
    val isByAi: Boolean = false,
    val step: Step? = null,
) {
    public enum class Step { UNDO, REDO }
}
