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
import java.util.concurrent.ConcurrentHashMap

/**
 * The files Kubriko Creator itself wrote or deleted in a project, as they were right after, so that [ProjectWatcher]
 * doesn't take Creator's own changes for someone else's and reload what Creator already has in memory. A file changed
 * again since counts as changed elsewhere.
 */
internal class OwnWrites {
    private val stamps = ConcurrentHashMap<File, FileStamp>()

    /**
     * Notes that Creator just wrote or deleted [file].
     */
    fun record(file: File) {
        stamps[file.absoluteFile] = FileStamp.of(file)
    }

    /**
     * Whether [file] is as Creator left it, judging by [stamp], its state now.
     */
    fun isOwn(file: File, stamp: FileStamp) = stamps[file.absoluteFile] == stamp
}
