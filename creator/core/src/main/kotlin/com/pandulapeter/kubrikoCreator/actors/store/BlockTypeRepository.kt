/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind

/**
 * Where a workspace's block-built Actor types and Manager types are kept.
 */
public interface BlockTypeRepository {

    /**
     * Reads every type, each file on its own, so that one that can't be read leaves the others readable.
     */
    public fun load(): LoadedBlockTypes

    /**
     * Writes [definition], leaving its code to [regenerate]. When [isReplacing], it stands for a [delete] of its name
     * followed by this save, which leaves the same files: every other definition file of the name, of either kind, goes,
     * and so does its generated code, changes made by hand included.
     */
    public fun save(definition: BlockTypeDefinition, isReplacing: Boolean = false)

    /**
     * Deletes the type called [name] and its generated code.
     */
    public fun delete(name: String)

    /**
     * Brings the generated code of [definitions] (every type of the project, as they are now) in line with them and with
     * what this version of Kubriko Creator generates, except where it was changed by hand, unless that type is in
     * [overwriting]. Code that didn't change isn't written, and generated files of types that no longer exist are deleted,
     * unless they were changed by hand.
     */
    public fun regenerate(definitions: List<BlockTypeDefinition>, overwriting: Set<String> = emptySet()): RegeneratedCode

    /**
     * [regenerate] with the definitions [load] reads, or nothing while any can't be read, since code generated without a
     * type would break the build of the types that name it.
     */
    public fun regenerateAll(overwriting: Set<String> = emptySet()): RegeneratedCode = load().let { loaded ->
        if (loaded.unreadableFiles.isEmpty()) regenerate(loaded.types, overwriting) else RegeneratedCode()
    }

    /**
     * Every folder that holds types of [kind], or held them, as paths (`enemies/flying`), parents included.
     */
    public fun folders(kind: BlockTypeKind): Set<String>

    public fun createFolder(kind: BlockTypeKind, path: String)

    /**
     * Deletes the folder at [path] if nothing is in it any more.
     */
    public fun deleteFolder(kind: BlockTypeKind, path: String)
}
