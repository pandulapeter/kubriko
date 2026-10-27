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
 * One step of the undo or redo history: the change that [revert] reverts and [reapply] makes again.
 */
internal data class HistoryEntry(
    val revert: Invocation<*>,
    val reapply: Invocation<*>,
) {
    /**
     * The change the user made, which names the step in both directions.
     */
    val title get() = reapply.title
}
