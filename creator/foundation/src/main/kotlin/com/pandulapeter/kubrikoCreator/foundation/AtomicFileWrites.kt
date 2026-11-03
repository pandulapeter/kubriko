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
import java.nio.file.AccessDeniedException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.CopyOption
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID

/**
 * What the temporary files of [writeTextAtomically] end with, which nothing watching the project counts as a change.
 */
public const val ATOMIC_WRITE_SUFFIX: String = ".creator-write.tmp"

/**
 * Replaces this file's content with [text] in one step, so a crash, a kill, or a full disk mid-write leaves either the old
 * content or the new one, never a truncated file. A link is followed, so it stays a link, and the file keeps its
 * permissions; a new one gets the usual ones, or, when [isPrivate], is readable by its owner only. Where the file system
 * can't move atomically, the file is replaced plainly. A move Windows refuses because another program has the file open is
 * tried again a few times first.
 */
public fun File.writeTextAtomically(text: String, isPrivate: Boolean = false) {
    val target = canonicalFile
    target.parentFile?.mkdirs()
    val temporary = File(target.parentFile, ".${target.name}.${UUID.randomUUID().toString().take(8)}$ATOMIC_WRITE_SUFFIX")
    try {
        temporary.createNewFile()
        val path = temporary.toPath()
        if (path.fileSystem.supportedFileAttributeViews().contains("posix")) {
            when {
                target.exists() -> Files.setPosixFilePermissions(path, Files.getPosixFilePermissions(target.toPath()))
                isPrivate -> Files.setPosixFilePermissions(path, OWNER_ONLY)
            }
        }
        temporary.writeText(text)
        moveReplacing(path, target.toPath())
    } finally {
        temporary.delete()
    }
}

/**
 * Moves [from] over [to], atomically where the file system can, trying again with a short backoff while Windows refuses it.
 */
internal fun moveReplacing(
    from: Path,
    to: Path,
    move: (Path, Path, Array<CopyOption>) -> Unit = { source, target, options -> Files.move(source, target, *options) },
    sleep: (Long) -> Unit = Thread::sleep,
) {
    var isAtomicMoveSupported = true
    var attempt = 0
    while (true) {
        try {
            if (isAtomicMoveSupported) {
                try {
                    move(from, to, arrayOf(StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE))
                    return
                } catch (_: AtomicMoveNotSupportedException) {
                    isAtomicMoveSupported = false
                }
            }
            move(from, to, arrayOf(StandardCopyOption.REPLACE_EXISTING))
            return
        } catch (exception: FileSystemException) {
            if (!exception.isRefusal() || attempt == MOVE_ATTEMPTS - 1) throw exception
            sleep(10L shl attempt)
            attempt++
        }
    }
}

/**
 * Windows reports a file another program holds open as access denied, or as a sharing violation through a plain
 * [FileSystemException]; its subclasses are real failures.
 */
private fun FileSystemException.isRefusal() = this is AccessDeniedException || javaClass == FileSystemException::class.java

private const val MOVE_ATTEMPTS = 5

/**
 * Makes this file readable and writable by its owner only, where the file system has POSIX permissions.
 */
public fun File.restrictToOwner() {
    val path = toPath()
    if (path.fileSystem.supportedFileAttributeViews().contains("posix")) Files.setPosixFilePermissions(path, OWNER_ONLY)
}

private val OWNER_ONLY = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
