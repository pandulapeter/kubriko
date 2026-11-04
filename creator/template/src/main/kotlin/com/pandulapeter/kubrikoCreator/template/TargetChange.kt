/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import java.security.MessageDigest

/**
 * What turning a game's target on or off takes.
 */
public sealed interface TargetChange {

    /**
     * The exact changes to the build files (and the target's files that are created), shown before they are made.
     *
     * @param isReversible Whether turning the target back afterwards brings every file it changes back as it was, keeping
     *   only the files it creates and what it adds to [TargetEdits.keptPaths].
     */
    public data class Proposed(
        public val target: GameTarget,
        public val isOn: Boolean,
        public val changes: List<ProposedFileChange>,
        public val isReversible: Boolean,
    ) : TargetChange {
        /**
         * Names exactly these changes, so that the user confirming them can be told from the files having changed since.
         */
        public val id: String by lazy {
            val digest = MessageDigest.getInstance("SHA-256")
            changes.forEach { change -> listOf(change.path, change.before, change.after.orEmpty(), change.resource.orEmpty()).forEach { digest.update((it + "\u0000").toByteArray()) } }
            digest.digest().joinToString("") { "%02x".format(it) }.take(16)
        }
    }

    /**
     * Why it can't be done, with the arguments of the message.
     */
    public data class Refused(
        public val problem: TargetProblem,
        public val arguments: List<String> = emptyList(),
    ) : TargetChange
}

/**
 * Whether a game is built for a target, and why it can't be changed or built here, if so, with the arguments of the message.
 */
public data class TargetState(
    public val target: GameTarget,
    public val isOn: Boolean,
    public val problem: TargetProblem? = null,
    public val arguments: List<String> = emptyList(),
)
