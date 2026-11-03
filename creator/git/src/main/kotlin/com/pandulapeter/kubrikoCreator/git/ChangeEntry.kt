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
 * A change as Kubriko Creator shows it: one file, or an Actor type or Manager type (its definition and its generated code
 * together).
 *
 * @param title The type's name, or the file's path.
 */
public data class ChangeEntry(
    public val title: String,
    public val kind: ChangeKind,
    public val paths: List<String>,
    public val isBlockType: Boolean,
)
