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
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID

/**
 * What the temporary files of [writeTextAtomically] end with, which nothing watching the project counts as a change.
 */
internal const val ATOMIC_WRITE_SUFFIX = ".creator-write.tmp"

/**
 * Replaces this file's content with [text] in one step, so a crash, a kill, or a full disk mid-write leaves either the old
 * content or the new one, never a truncated file. A link is followed, so it stays a link, and the file keeps its
 * permissions; a new one gets the usual ones, or, when [isPrivate], is readable by its owner only. Where the file system
 * can't move atomically, the file is replaced plainly.
 */
internal fun File.writeTextAtomically(text: String, isPrivate: Boolean = false) {
    val target = canonicalFile
    target.parentFile?.mkdirs()
    val temporary = File(target.parentFile, ".${target.name}.${UUID.randomUUID().toString().take(8)}$ATOMIC_WRITE_SUFFIX")
    try {
        temporary.createNewFile()
        val path = temporary.toPath()
        if (path.fileSystem.supportedFileAttributeViews().contains("posix")) {
            when {
                target.exists() -> Files.setPosixFilePermissions(path, Files.getPosixFilePermissions(target.toPath()))
                isPrivate -> Files.setPosixFilePermissions(path, setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))
            }
        }
        temporary.writeText(text)
        try {
            Files.move(path, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(path, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    } finally {
        temporary.delete()
    }
}
