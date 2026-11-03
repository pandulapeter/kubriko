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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.flow.StateFlow

/**
 * The block-built Actor types and Manager types of a project that opted in to them, as commands read and change them.
 * Changes show up immediately and are written in the background.
 */
public interface BlockTypes {

    /**
     * Every Actor type and Manager type, sorted by name.
     */
    public val types: StateFlow<List<BlockTypeDefinition>>
    public val actorTypes: StateFlow<List<ActorTypeDefinition>>
    public val managerTypes: StateFlow<List<ManagerTypeDefinition>>

    /**
     * The types whose generated code was changed by hand (in IntelliJ, for example), and which Kubriko Creator
     * therefore stopped updating until those changes are discarded ([discardChangesMadeByHand]).
     */
    public val changedByHand: StateFlow<Set<String>>

    /**
     * The definition files that couldn't be read when they were last loaded, which are left out of [types].
     */
    public val unreadableFiles: StateFlow<List<UnreadableBlockTypeFile>>

    /**
     * Every folder of each kind of type, as paths (`enemies/flying`), parents included: those types are in, and empty ones.
     */
    public val folders: StateFlow<Map<BlockTypeKind, Set<String>>>

    /**
     * The names no new type may have: every type's, and those of the files that couldn't be read, which would clash with
     * it once fixed.
     */
    public val takenNames: List<String>

    /**
     * The Actor type or Manager type called [name].
     */
    public operator fun get(name: String): BlockTypeDefinition?

    public fun actorType(name: String): ActorTypeDefinition?

    public fun managerType(name: String): ManagerTypeDefinition?

    /**
     * Adds [definition], or replaces the type of the same name.
     */
    @EditorMutation
    public fun put(definition: BlockTypeDefinition)

    @EditorMutation
    public fun createFolder(kind: BlockTypeKind, path: String)

    /**
     * Deletes the folder at [path], which must hold no types (in it or in its folders).
     */
    @EditorMutation
    public fun deleteFolder(kind: BlockTypeKind, path: String)

    @EditorMutation
    public fun delete(name: String)

    /**
     * Renames [oldName] to [newName], in the Events and Blocks of every type that refer to it too, keeping it selected if
     * it was.
     */
    @EditorMutation
    public fun rename(oldName: String, newName: String)

    /**
     * Throws away the changes made by hand to [name]'s generated code, so that Kubriko Creator updates it again.
     */
    @EditorMutation
    public fun discardChangesMadeByHand(name: String)

    /**
     * Returns once every change made so far is written, or writing them failed; whether they all are on disk.
     */
    public suspend fun awaitWrites(): Boolean

    /**
     * What isn't on disk yet, and why, for telling the user what an action that reads the files goes ahead without.
     */
    public fun unsavedWrites(): UnsavedWrites

    /**
     * Reads the definitions again after they changed outside Kubriko Creator, returning once they are in memory, unless a
     * change of a type was queued while they were read, which is then read again.
     */
    public suspend fun reload()
}
