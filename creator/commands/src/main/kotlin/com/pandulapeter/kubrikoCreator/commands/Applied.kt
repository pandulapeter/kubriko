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

import com.pandulapeter.kubrikoCreator.foundation.DomainText
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * The result of applying a [WriteCommand].
 *
 * @param inverse The command that reverts the change, or null if it can't be undone (a git commit, for example).
 * @param result What the change produced, for the AI (the index of a placed Actor, for example).
 * @param failure Why the change couldn't be made after all, though it was valid (the disk or git failed, for example).
 * @param details What the AI hears about the change besides what it changed, in the summary a change without a [result]
 *   returns (what a delete cleaned up, for example).
 * @param isRewritingFiles Whether the change rewrote the project's files without an inverse (discarding changes through
 *   git, for example), which leaves the steps recorded against the files before it pointless or wrong, so the history is
 *   cleared.
 */
public data class Applied(
    val inverse: Invocation<*>?,
    val result: JsonElement = JsonNull,
    val failure: Rejection? = null,
    val isRewritingFiles: Boolean = false,
    val details: JsonObject? = null,
) {
    public companion object {

        internal fun failed(reason: DomainText) = failed(Rejection(reason))

        internal fun failed(rejection: Rejection) = Applied(inverse = null, failure = rejection)
    }
}
