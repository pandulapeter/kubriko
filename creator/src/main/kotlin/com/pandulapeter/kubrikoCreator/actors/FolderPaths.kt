/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_folder_name_invalid
import org.jetbrains.compose.resources.StringResource

/**
 * The rules for folders in the sidebar, which are paths of folder names separated by `/` (`enemies/flying`), empty for
 * none (the top of a group).
 */
internal object FolderPaths {

    private val namePattern = Regex("[A-Za-z0-9][A-Za-z0-9 _-]*")

    /**
     * Why [name] can't be a folder's name, or null if it can.
     */
    fun validateName(name: String): StringResource? = if (namePattern.matches(name) && name == name.trim()) null else Res.string.error_folder_name_invalid

    fun isValid(path: String) = path.isEmpty() || path.split('/').all { validateName(it) == null }

    fun parentOf(path: String) = path.substringBeforeLast('/', "")

    fun nameOf(path: String) = path.substringAfterLast('/')

    fun child(parent: String, name: String) = if (parent.isEmpty()) name else "$parent/$name"

    /**
     * Whether [path] is [folder] or inside it.
     */
    fun isWithin(path: String, folder: String) = path == folder || path.startsWith("$folder/")
}

/**
 * These folders and every folder they are in.
 */
internal fun Set<String>.withParents(): Set<String> = flatMapTo(sortedSetOf()) { path ->
    path.split('/').runningReduce { parent, name -> "$parent/$name" }
}
