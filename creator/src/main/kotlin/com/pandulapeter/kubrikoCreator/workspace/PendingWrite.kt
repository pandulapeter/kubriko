/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind

/**
 * A change to the block-built types' files that [BlockTypesController] hasn't written yet. Every batch of them ends with
 * the code regenerated, so [Regenerate] asks for nothing more, and [Overwrite] for that type's hand-made changes to go.
 */
internal sealed interface PendingWrite {

    /**
     * What this write changes on disk by itself, or null for the ones that take effect only in the regeneration.
     */
    val subject: FailedWrite.Subject?

    /**
     * Writes [definition], standing for a [Delete] of its name before it too when [isReplacing].
     */
    data class Save(val definition: BlockTypeDefinition, val isReplacing: Boolean = false) : PendingWrite {
        override val subject get() = FailedWrite.Subject.Type(definition.name)
    }

    data class Delete(val name: String) : PendingWrite {
        override val subject get() = FailedWrite.Subject.Type(name)
    }

    data class CreateFolder(val kind: BlockTypeKind, val path: String) : PendingWrite {
        override val subject get() = FailedWrite.Subject.Folder(path)
    }

    data class DeleteFolder(val kind: BlockTypeKind, val path: String) : PendingWrite {
        override val subject get() = FailedWrite.Subject.Folder(path)
    }

    data object Regenerate : PendingWrite {
        override val subject = null
    }

    data class Overwrite(val name: String) : PendingWrite {
        override val subject = null
    }

    companion object {

        /**
         * [writes] with each type saved once: a save followed by a later save of the same type, with no delete of it in
         * between, keeps its own place with the later definition, and one followed by a later delete of it is dropped. A
         * delete followed by a later save of the same type becomes that save, in the delete's place, replacing what the
         * delete would have removed, so that the type's file is never gone while the save waits. A save never moves after
         * a write that came after it, so a renamed type's old file is still deleted only after the new ones are written.
         */
        fun withoutOverwrittenSaves(writes: List<PendingWrite>): List<PendingWrite> {
            val merged = ArrayList<PendingWrite?>(writes.size)
            val openSaveIndices = HashMap<String, Int>()
            val openDeleteIndices = HashMap<String, Int>()
            writes.forEach { write ->
                when (write) {
                    is Save -> {
                        val name = write.definition.name
                        val saveIndex = openSaveIndices[name]
                        val deleteIndex = openDeleteIndices.remove(name)
                        when {
                            saveIndex != null -> merged[saveIndex] = write.copy(isReplacing = (merged[saveIndex] as Save).isReplacing)
                            deleteIndex != null -> {
                                merged[deleteIndex] = write.copy(isReplacing = true)
                                openSaveIndices[name] = deleteIndex
                            }
                            else -> {
                                openSaveIndices[name] = merged.size
                                merged += write
                            }
                        }
                    }
                    is Delete -> {
                        openSaveIndices.remove(write.name)?.let { index -> merged[index] = null }
                        if (write.name !in openDeleteIndices) {
                            openDeleteIndices[write.name] = merged.size
                            merged += write
                        }
                    }
                    else -> merged += write
                }
            }
            return merged.filterNotNull()
        }
    }
}
