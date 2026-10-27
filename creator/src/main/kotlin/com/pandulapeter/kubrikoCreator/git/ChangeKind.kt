/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.git

/**
 * How a file differs from the last commit.
 */
internal enum class ChangeKind {
    ADDED,
    MODIFIED,
    DELETED,

    /**
     * Left in conflict by a merge that git couldn't finish on its own.
     */
    CONFLICTING,
}
