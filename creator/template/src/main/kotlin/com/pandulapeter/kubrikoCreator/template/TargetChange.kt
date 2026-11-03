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

/**
 * What turning a game's target on or off takes.
 */
public sealed interface TargetChange {

    /**
     * The exact changes to the build files (and the target's files that are created), shown before they are made.
     */
    public data class Proposed(
        public val target: GameTarget,
        public val isOn: Boolean,
        public val changes: List<ProposedFileChange>,
    ) : TargetChange

    /**
     * Why it can't be done, with the arguments of the message.
     */
    public data class Refused(
        public val problem: TargetProblem,
        public val arguments: List<String> = emptyList(),
    ) : TargetChange
}

/**
 * Whether a game is built for a target, and why it can't be changed or built here, if so.
 */
public data class TargetState(
    public val target: GameTarget,
    public val isOn: Boolean,
    public val problem: TargetProblem? = null,
)
