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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeRepository
import com.pandulapeter.kubrikoCreator.actors.RegeneratedCode
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges
import kotlinx.coroutines.CancellationException

/**
 * The part of [BlockTypesController]'s writing that touches the disk, called off the main thread.
 */
internal class BlockTypeBatchWriter(
    private val repository: BlockTypeRepository,
    private val syncBuildFiles: () -> KubrikoBuildFileChanges?,
) {

    /**
     * Writes [batch] in order, stopping at the first write that fails, and regenerates the code from [types] once all are
     * written, unless [unregenerated] stands in for the regeneration.
     */
    fun write(batch: List<PendingWrite>, types: List<BlockTypeDefinition>, unregenerated: RegeneratedCode?): BatchResult {
        batch.forEachIndexed { index, write ->
            try {
                when (write) {
                    is PendingWrite.Save -> repository.save(write.definition)
                    is PendingWrite.Delete -> repository.delete(write.name)
                    is PendingWrite.CreateFolder -> repository.createFolder(write.kind, write.path)
                    is PendingWrite.DeleteFolder -> repository.deleteFolder(write.kind, write.path)
                    PendingWrite.Regenerate, is PendingWrite.Overwrite -> Unit
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                return BatchResult(writtenCount = index, failure = BatchFailure(write, exception))
            }
        }
        if (unregenerated != null) return BatchResult(writtenCount = batch.size, regenerated = unregenerated)
        return try {
            val regenerated = repository.regenerate(types, overwriting = batch.filterIsInstance<PendingWrite.Overwrite>().mapTo(mutableSetOf()) { it.name })
            BatchResult(writtenCount = batch.size, regenerated = regenerated, buildFileChanges = syncBuildFiles())
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            BatchResult(writtenCount = batch.size, failure = BatchFailure(write = null, exception = exception))
        }
    }
}

/**
 * How a batch of writes went: how many of its writes are on disk, and either what failed or the regeneration that followed.
 */
internal class BatchResult(
    val writtenCount: Int,
    val failure: BatchFailure? = null,
    val regenerated: RegeneratedCode? = null,
    val buildFileChanges: KubrikoBuildFileChanges? = null,
) {

    /**
     * What of [batch] has to be written again after it failed: the writes from the failing one on, and the
     * [PendingWrite.Overwrite]s and the regeneration the failure skipped.
     */
    fun unwrittenPartOf(batch: List<PendingWrite>) = buildList {
        addAll(batch.take(writtenCount).filterIsInstance<PendingWrite.Overwrite>())
        addAll(batch.drop(writtenCount))
        if (failure?.write == null) add(PendingWrite.Regenerate)
    }
}

/**
 * The write of a batch that failed, or a null [write] when the regeneration after it did.
 */
internal class BatchFailure(
    val write: PendingWrite?,
    val exception: Exception,
)
