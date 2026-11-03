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
 * What a project's own build makes, as opposed to the libraries it downloads: anything under the project's folder.
 * An included build outside that folder counts as a library, so its outputs are used where they are.
 */
public object ProjectOutput {

    internal fun isOwn(file: File, rootDirectory: File) = isOwnUnderCanonicalRoot(file, rootDirectory.canonicalFile)

    /**
     * [isOwn] for a [canonicalRoot] that is already canonical, for callers checking many files against one root.
     */
    public fun isOwnUnderCanonicalRoot(file: File, canonicalRoot: File): Boolean = file.canonicalFile.startsWith(canonicalRoot)
}
