/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubrikoCreator.actors.NameFlaw
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.project.ProjectTree

/**
 * The rules for Scene names, which are paths: folder names and the file's name separated by `/` (`levels/1-1`).
 */
public object SceneNaming {

    public fun isValid(name: String): Boolean = validate(name) == null

    /**
     * Why [name] can't be a Scene's name (or a Scene folder's path), or null if it can.
     */
    public fun validate(name: String): NameProblem? = name.split('/').let { parts ->
        parts.firstNotNullOfOrNull { part ->
            when (NameFlaw.of(part, NameFlaw::isLetterOrDigitAToZ) { NameFlaw.isLetterOrDigitAToZ(it) || it in "_-" }) {
                NameFlaw.EMPTY -> if (parts.size > 1) NameProblem.PATH_PART_EMPTY else NameProblem.NAME_EMPTY
                NameFlaw.LETTER_BEYOND_A_TO_Z -> NameProblem.NAME_LETTER_BEYOND_A_TO_Z
                NameFlaw.FIRST_CHARACTER -> NameProblem.SCENE_FIRST_CHARACTER
                NameFlaw.CHARACTER -> NameProblem.SCENE_CHARACTERS
                null -> null
            }
        }
    }

    /**
     * The first folder of [name] that Kubriko Creator skips, as build tools do, so a Scene in it would never show up, or
     * null. When [isFolder], [name] is a folder, so its last part counts too.
     */
    public fun reservedFolderOf(name: String, isFolder: Boolean = false): String? = name.split('/')
        .let { parts -> if (isFolder) parts else parts.dropLast(1) }
        .firstOrNull { it in ProjectTree.ignoredDirectoryNames }
}
