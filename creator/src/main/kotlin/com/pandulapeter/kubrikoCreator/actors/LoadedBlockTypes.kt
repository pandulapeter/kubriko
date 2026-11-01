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

/**
 * What [BlockTypeRepository.load] found: the types it could read, sorted by name, and the files it couldn't.
 */
internal data class LoadedBlockTypes(
    val types: List<BlockTypeDefinition>,
    val unreadableFiles: List<UnreadableBlockTypeFile> = emptyList(),
)

/**
 * A type definition that couldn't be read (a merge conflict, a crash mid-write, or a hand edit) or can't be used as it is.
 *
 * @param path The file's path, relative to the project root.
 */
internal data class UnreadableBlockTypeFile(
    val path: String,
    val problem: BlockTypeFileProblem,
) {
    /**
     * The name the file gives its type, which stays taken while the file is unreadable.
     */
    val name get() = path.substringAfterLast('/').substringBeforeLast('.')
}

/**
 * Why an [UnreadableBlockTypeFile] was left out.
 */
internal sealed interface BlockTypeFileProblem {

    /**
     * The file isn't a definition at all; [message] says why, on one line.
     */
    data class Unparsable(val message: String) : BlockTypeFileProblem

    /**
     * The type inside is called [name], which isn't the file's name.
     */
    data class NameMismatch(val name: String) : BlockTypeFileProblem

    /**
     * [name] can't be a Kotlin class name the way a type's name has to be.
     */
    data class InvalidName(val name: String) : BlockTypeFileProblem

    /**
     * [name] is used by Kubriko or by the code Kubriko Creator generates.
     */
    data class ReservedName(val name: String) : BlockTypeFileProblem

    /**
     * Other files, at [otherPaths], hold a type of the same name (ignoring case), so none of them is loaded.
     */
    data class DuplicateName(val otherPaths: List<String>) : BlockTypeFileProblem
}
