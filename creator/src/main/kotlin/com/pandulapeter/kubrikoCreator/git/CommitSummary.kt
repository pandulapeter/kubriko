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
 * One commit of the history.
 *
 * @param id The commit's abbreviated hash.
 * @param time When it was committed, in milliseconds since the epoch.
 */
internal data class CommitSummary(
    val id: String,
    val message: String,
    val author: String,
    val time: Long,
)
