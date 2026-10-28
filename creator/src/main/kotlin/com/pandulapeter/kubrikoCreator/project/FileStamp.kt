/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import java.io.File

/**
 * What tells a file's versions apart without reading it: its length and modification time, or neither for a file that
 * doesn't exist ([MISSING]).
 */
internal data class FileStamp(
    val length: Long,
    val lastModified: Long,
) {
    companion object {
        val MISSING = FileStamp(length = -1, lastModified = -1)

        fun of(file: File) = if (file.isFile) FileStamp(file.length(), file.lastModified()) else MISSING
    }
}
