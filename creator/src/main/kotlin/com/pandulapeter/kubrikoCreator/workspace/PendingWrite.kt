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

    data class Save(val definition: BlockTypeDefinition) : PendingWrite

    data class Delete(val name: String) : PendingWrite

    data class CreateFolder(val kind: BlockTypeKind, val path: String) : PendingWrite

    data class DeleteFolder(val kind: BlockTypeKind, val path: String) : PendingWrite

    data object Regenerate : PendingWrite

    data class Overwrite(val name: String) : PendingWrite

    companion object {

        /**
         * [writes] without the saves that a later save or delete of the same type makes pointless, in their order.
         */
        fun withoutOverwrittenSaves(writes: List<PendingWrite>): List<PendingWrite> {
            val laterTypeNames = HashSet<String>()
            return writes.asReversed().filter { write ->
                when (write) {
                    is Save -> laterTypeNames.add(write.definition.name)
                    is Delete -> true.also { laterTypeNames += write.name }
                    else -> true
                }
            }.asReversed()
        }
    }
}
