/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.UUID

/**
 * Moves this file to [target], creating the folders [target] needs, and never over another file. A move that changes
 * only the case of the name (onto the same file, where names ignore case) goes through a temporary name, since macOS
 * keeps the old name when a file is renamed straight to it.
 *
 * @return Whether it was moved: Windows refuses to move a file another program has open.
 */
public fun File.moveWithoutReplacing(target: File): Boolean = try {
    target.parentFile?.mkdirs()
    if (target.exists() && Files.isSameFile(toPath(), target.toPath())) {
        if (path != target.path) {
            val temporary = File(parentFile, ".$name.${UUID.randomUUID().toString().take(8)}$ATOMIC_WRITE_SUFFIX")
            Files.move(toPath(), temporary.toPath())
            try {
                Files.move(temporary.toPath(), target.toPath())
            } catch (exception: IOException) {
                Files.move(temporary.toPath(), toPath())
                throw exception
            }
        }
    } else {
        Files.move(toPath(), target.toPath())
    }
    true
} catch (_: IOException) {
    false
}
