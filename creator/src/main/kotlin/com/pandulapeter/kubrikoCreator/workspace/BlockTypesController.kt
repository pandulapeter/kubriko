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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeFileProblem
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeRepository
import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.RegeneratedCode
import com.pandulapeter.kubrikoCreator.actors.UnreadableBlockTypeFile
import com.pandulapeter.kubrikoCreator.actors.withManagerRenamed
import com.pandulapeter.kubrikoCreator.actors.withOtherActorTypeRenamed
import com.pandulapeter.kubrikoCreator.actors.withParents
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The block-built Actor types and Manager types of a project that opted in to them, and the one being edited.
 *
 * Changes show up immediately; writing them happens in the background, in order, in batches: the changes made within
 * [writeDelayInMilliseconds] of each other (typing into a field, for example) are written together, a type saved
 * several times is written once, and the code is regenerated once per batch, from the types in memory. After each
 * batch, [syncBuildFiles] brings the project's Kubriko dependencies in line with the code. A write that fails stays queued
 * with everything after it, in order (so a renamed type's old file is deleted only once the new ones are written, and a
 * deleted type isn't read back), and is tried again with the next change, or after a while; nothing is regenerated until
 * it succeeds.
 *
 * While a definition file can't be read, the code isn't regenerated, nor are the build files synced, since code generated
 * without that type would break the build; the file is never written over or deleted. Neither happens while [isReadOnly]
 * either: the project was saved by a newer Kubriko Creator, whose code this one would generate wrongly.
 *
 * @param onSaved Called after each batch, with whether the types changed, rather than only the Scenes they were
 *   regenerated for.
 * @param onSaveFailed Called with what a batch couldn't write, once until the failure changes.
 * @param onSavedAgain Called when a batch succeeds after one failed.
 * @param onFileUnreadable Called for each definition file found unreadable, once until it changes.
 * @param onFilesReadableAgain Called when no definition file is unreadable any more.
 * @param onLeftoversRemoved Called with the generated files (by path) a regeneration deleted, since their types no longer exist.
 * @param onLeftoverChangedByHand Called for each generated file (by path) whose type no longer exists but which was
 *   changed by hand, so it was kept, once until it is gone.
 */
internal class BlockTypesController(
    private val repository: BlockTypeRepository,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val onSaved: (haveTypesChanged: Boolean) -> Unit,
    private val onSaveFailed: (FailedWrite) -> Unit,
    private val onSavedAgain: () -> Unit = {},
    private val onFileUnreadable: (UnreadableBlockTypeFile) -> Unit = {},
    private val onFirstLoaded: (List<BlockTypeDefinition>) -> Unit = {},
    private val onFilesReadableAgain: () -> Unit = {},
    private val onLeftoversRemoved: (List<String>) -> Unit = {},
    private val onLeftoverChangedByHand: (String) -> Unit = {},
    syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
    private val onBuildFilesSynced: (KubrikoBuildFileChanges) -> Unit = {},
    private val writeDelayInMilliseconds: Long = 0,
    private val isReadOnly: () -> Boolean = { false },
) {
    private val _types = MutableStateFlow(emptyList<BlockTypeDefinition>())

    /**
     * Every Actor type and Manager type, sorted by name.
     */
    val types = _types.asStateFlow()
    private val _actorTypes = MutableStateFlow(emptyList<ActorTypeDefinition>())
    val actorTypes = _actorTypes.asStateFlow()
    private val _managerTypes = MutableStateFlow(emptyList<ManagerTypeDefinition>())
    val managerTypes = _managerTypes.asStateFlow()
    private val _selectedName = MutableStateFlow<String?>(null)
    val selectedName = _selectedName.asStateFlow()
    private val _changedByHand = MutableStateFlow(emptySet<String>())

    /**
     * The types whose generated code was changed by hand (in IntelliJ, for example), and which Kubriko Creator
     * therefore stopped updating until those changes are discarded ([discardChangesMadeByHand]).
     */
    val changedByHand = _changedByHand.asStateFlow()
    private val _unreadableFiles = MutableStateFlow(emptyList<UnreadableBlockTypeFile>())

    /**
     * The definition files that couldn't be read when they were last loaded, which are left out of [types].
     */
    val unreadableFiles = _unreadableFiles.asStateFlow()

    /**
     * The folders that exist without a type in them, by the kind of type they hold.
     */
    private var emptyFolders = emptyMap<BlockTypeKind, Set<String>>()
    private val _folders = MutableStateFlow(emptyMap<BlockTypeKind, Set<String>>())

    /**
     * Every folder of each kind of type, as paths (`enemies/flying`), parents included: those types are in, and empty ones.
     */
    val folders = _folders.asStateFlow()

    /**
     * The generated files of types that no longer exist that the last regeneration kept, since they were changed by hand.
     */
    private var leftoversChangedByHand = emptySet<String>()
    private val batchWriter = BlockTypeBatchWriter(repository, syncBuildFiles)
    private val pendingWrites = ArrayList<PendingWrite>()

    /**
     * The batch being written, which counts as unwritten until it is back on the main thread.
     */
    private var inFlight = emptyList<PendingWrite>()
    private val hasUnwritten get() = pendingWrites.isNotEmpty() || inFlight.isNotEmpty()
    private var havePendingTypesChanged = false
    private var isReloadRequested = false
    private val writeRequests = Channel<Unit>(Channel.CONFLATED)
    private var retryJob: Job? = null
    private var consecutiveFailures = 0

    /**
     * Raised for every queued write; a batch covers every generation up to the one current when it was taken.
     */
    private var queuedGeneration = 0L
    private val attemptedGeneration = MutableStateFlow(0L)
    private var writtenGeneration = 0L

    /**
     * Raised for every queued write that changes a definition or a folder, so a reload knows its result may be outdated.
     */
    private var typeGeneration = 0L
    private val _unwrittenCount = MutableStateFlow(0)

    /**
     * How many queued writes aren't on disk yet, the failed and the one being written included.
     */
    val unwrittenCount = _unwrittenCount.asStateFlow()
    private val _failure = MutableStateFlow<FailedWrite?>(null)

    /**
     * What the last batch couldn't write, or null if it succeeded.
     */
    val failure = _failure.asStateFlow()

    init {
        // The generated code of a project last opened in an older Kubriko Creator is brought up to date right away,
        // before the first build, so no classes loaded from it are outdated.
        queue(PendingWrite.Regenerate, haveTypesChanged = false)
        scope.launch {
            try {
                val loaded = withContext(ioDispatcher) { repository.load() }
                emptyFolders = withContext(ioDispatcher) { BlockTypeKind.entries.associateWith(repository::folders) }
                setUnreadableFiles(loaded.unreadableFiles)
                setTypes(loaded.types)
                onFirstLoaded(_types.value)
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                setUnreadableFiles(listOf(UnreadableBlockTypeFile(BlockTypeStore.CREATOR_DIRECTORY, BlockTypeFileProblem.Unparsable(exception.message ?: exception.toString()))))
            }
            for (request in writeRequests) {
                delay(writeDelayInMilliseconds)
                try {
                    writePending()
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    currentCoroutineContext()[CoroutineExceptionHandler]?.handleException(currentCoroutineContext(), exception)
                }
            }
        }
    }

    /**
     * Returns once every change made so far is written, or writing them failed; whether they all are on disk.
     */
    suspend fun awaitWrites(): Boolean {
        val wanted = queuedGeneration
        attemptedGeneration.first { it >= wanted }
        return writtenGeneration >= wanted
    }

    /**
     * Returns once no change is left to write, however many times a failed one has to be tried again.
     */
    suspend fun awaitWritten() {
        _unwrittenCount.first { it == 0 }
    }

    /**
     * The types whose changes aren't on disk yet, sorted.
     */
    fun unsavedNames() = (inFlight + pendingWrites).mapNotNull { (it.subject as? FailedWrite.Subject.Type)?.name }.distinct().sorted()

    /**
     * What isn't on disk yet, and why, for telling the user what an action that reads the files goes ahead without.
     */
    fun unsavedWrites() = UnsavedWrites(
        subjects = unsavedNames().joinToString().ifEmpty { (_failure.value?.subject as? FailedWrite.Subject.Folder)?.path },
        reason = _failure.value?.reason.orEmpty(),
    )

    fun select(name: String?) {
        _selectedName.value = name
    }

    /**
     * The names no new type may have: every type's, and those of the files that couldn't be read, which would clash with
     * it once fixed.
     */
    val takenNames get() = _types.value.map { it.name } + _unreadableFiles.value.map { it.name }

    /**
     * The Actor type or Manager type called [name].
     */
    operator fun get(name: String) = _types.value.firstOrNull { it.name == name }

    fun actorType(name: String) = get(name) as? ActorTypeDefinition

    fun managerType(name: String) = get(name) as? ManagerTypeDefinition

    /**
     * Adds [definition], or replaces the type of the same name.
     */
    @EditorMutation
    fun put(definition: BlockTypeDefinition) {
        // A type moved out of a folder leaves the folder behind, empty or not.
        get(definition.name)?.takeIf { it.folder.isNotEmpty() && it.folder != definition.folder }?.let { previous ->
            emptyFolders = emptyFolders.withFolder(previous.kind, previous.folder)
        }
        setTypes(_types.value.filter { it.name != definition.name } + definition)
        queue(PendingWrite.Save(definition))
    }

    @EditorMutation
    fun createFolder(kind: BlockTypeKind, path: String) {
        emptyFolders = emptyFolders.withFolder(kind, path)
        setTypes(_types.value)
        queue(PendingWrite.CreateFolder(kind, path), haveTypesChanged = false)
    }

    /**
     * Deletes the folder at [path], which must hold no types (in it or in its folders).
     */
    @EditorMutation
    fun deleteFolder(kind: BlockTypeKind, path: String) {
        emptyFolders = emptyFolders + (kind to emptyFolders[kind].orEmpty().filterTo(mutableSetOf()) { it != path && !it.startsWith("$path/") })
        setTypes(_types.value)
        queue(PendingWrite.DeleteFolder(kind, path), haveTypesChanged = false)
    }

    @EditorMutation
    fun delete(name: String) {
        setTypes(_types.value.filter { it.name != name })
        if (_selectedName.value == name) _selectedName.value = null
        queue(PendingWrite.Delete(name))
    }

    /**
     * Renames [oldName] to [newName], in the Events and Blocks of every type that refer to it too, keeping it selected if
     * it was.
     */
    @EditorMutation
    fun rename(oldName: String, newName: String) {
        val isManagerType = get(oldName) is ManagerTypeDefinition
        val renamed = _types.value.mapNotNull { definition ->
            val withReferences = if (isManagerType) definition.withManagerRenamed(oldName, newName) else definition.withOtherActorTypeRenamed(oldName, newName)
            when {
                definition.name == oldName -> withReferences.withName(newName)
                withReferences != definition -> withReferences
                else -> null
            }
        }
        setTypes(_types.value.filter { definition -> definition.name != oldName && renamed.none { it.name == definition.name } } + renamed)
        if (_selectedName.value == oldName) _selectedName.value = newName
        renamed.forEach { definition -> queue(PendingWrite.Save(definition)) }
        queue(PendingWrite.Delete(oldName))
    }

    /**
     * Throws away the changes made by hand to [name]'s generated code, so that Kubriko Creator updates it again.
     */
    @EditorMutation
    fun discardChangesMadeByHand(name: String) = queue(PendingWrite.Overwrite(name))

    /**
     * Brings the generated code in line with the types and the project's Scenes: after the Scenes changed, since a Scene's
     * music decides whether the game plays audio, and once the project may be changed again.
     */
    fun regenerate() = queue(PendingWrite.Regenerate, haveTypesChanged = false)

    /**
     * Reads the definitions again after they changed outside Kubriko Creator (in IntelliJ, or through git), unless one
     * of Creator's own changes is still being written, in which case they are read once it is. While a write keeps
     * failing, they are read only after it succeeds, so the retried write wins over an outside change to the same file.
     */
    fun reloadFromDisk() {
        if (hasUnwritten) {
            isReloadRequested = true
            return
        }
        scope.launch { reload() }
    }

    /**
     * Reads the definitions again, as [reloadFromDisk] does, returning once they are in memory, unless a change of a type
     * was queued while they were read, which is then read again.
     */
    suspend fun reload() {
        repeat(RELOAD_ATTEMPTS) {
            if (hasUnwritten) {
                isReloadRequested = true
                return
            }
            val generation = typeGeneration
            val (loaded, storedFolders) = try {
                withContext(ioDispatcher) { repository.load() to BlockTypeKind.entries.associateWith(repository::folders) }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                return
            }
            if (generation == typeGeneration && pendingWrites.none { it.subject != null }) {
                if (loaded.types != _types.value || storedFolders != emptyFolders || loaded.unreadableFiles != _unreadableFiles.value) {
                    emptyFolders = storedFolders
                    setUnreadableFiles(loaded.unreadableFiles)
                    setTypes(loaded.types)
                    if (_selectedName.value !in loaded.types.map { it.name }) _selectedName.value = null
                    queue(PendingWrite.Regenerate)
                }
                return
            }
        }
        isReloadRequested = true
    }

    private fun setUnreadableFiles(files: List<UnreadableBlockTypeFile>) {
        val previous = _unreadableFiles.value
        _unreadableFiles.value = files
        files.filter { it !in previous }.forEach(onFileUnreadable)
        if (files.isEmpty() && previous.isNotEmpty()) onFilesReadableAgain()
    }

    private fun setTypes(types: List<BlockTypeDefinition>) {
        val sorted = types.sortedBy { it.name }
        _types.value = sorted
        _actorTypes.value = sorted.filterIsInstance<ActorTypeDefinition>()
        _managerTypes.value = sorted.filterIsInstance<ManagerTypeDefinition>()
        _folders.value = BlockTypeKind.entries.associateWith { kind ->
            sorted.filter { it.kind == kind && it.folder.isNotEmpty() }.fold(emptyFolders[kind].orEmpty().withParents()) { folders, type -> folders + setOf(type.folder).withParents() }
        }
    }

    private fun Map<BlockTypeKind, Set<String>>.withFolder(kind: BlockTypeKind, path: String) = this + (kind to get(kind).orEmpty() + path)

    private fun queue(write: PendingWrite, haveTypesChanged: Boolean = true) {
        pendingWrites += write
        if (haveTypesChanged) havePendingTypesChanged = true
        queuedGeneration++
        if (write.subject != null) typeGeneration++
        updateUnwrittenCount()
        retryJob?.cancel()
        writeRequests.trySend(Unit)
    }

    private fun updateUnwrittenCount() {
        _unwrittenCount.value = pendingWrites.size + inFlight.size
    }

    /**
     * Writes every change queued so far as one batch, then regenerates the code from the types as they are in memory.
     */
    private suspend fun writePending() {
        if (pendingWrites.isEmpty()) return
        val batch = PendingWrite.withoutOverwrittenSaves(pendingWrites.toList())
        pendingWrites.clear()
        // This batch covers the requests made so far, so a failed one waits for its retry rather than going again at once.
        writeRequests.tryReceive()
        inFlight = batch
        val takenGeneration = queuedGeneration
        val haveTypesChanged = havePendingTypesChanged
        havePendingTypesChanged = false
        var result: BatchResult? = null
        try {
            val types = _types.value
            val unregenerated = if (_unreadableFiles.value.isEmpty() && !isReadOnly()) null else RegeneratedCode(changedByHand = _changedByHand.value, leftoversChangedByHand = leftoversChangedByHand)
            result = withContext(ioDispatcher) { batchWriter.write(batch, types, unregenerated) }
        } finally {
            inFlight = emptyList()
            val completed = result
            if (completed?.failure != null) {
                pendingWrites.addAll(0, completed.unwrittenPartOf(batch))
                havePendingTypesChanged = havePendingTypesChanged || haveTypesChanged
            } else if (completed != null) {
                writtenGeneration = takenGeneration
            }
            updateUnwrittenCount()
            attemptedGeneration.value = takenGeneration
        }
        try {
            val completed = checkNotNull(result)
            val failure = completed.failure
            val regenerated = completed.regenerated
            if (failure != null) {
                reportFailure(FailedWrite.of(failure.write?.subject ?: FailedWrite.Subject.GeneratedCode, failure.exception))
            } else if (regenerated != null) {
                consecutiveFailures = 0
                if (_failure.value != null) {
                    _failure.value = null
                    onSavedAgain()
                }
                _changedByHand.value = regenerated.changedByHand
                if (regenerated.removedLeftovers.isNotEmpty()) onLeftoversRemoved(regenerated.removedLeftovers)
                regenerated.leftoversChangedByHand.filter { it !in leftoversChangedByHand }.sorted().forEach(onLeftoverChangedByHand)
                leftoversChangedByHand = regenerated.leftoversChangedByHand
                completed.buildFileChanges?.let(onBuildFilesSynced)
                onSaved(haveTypesChanged)
            }
        } finally {
            if (isReloadRequested && !hasUnwritten) {
                isReloadRequested = false
                scope.launch { reload() }
            }
        }
    }

    private fun reportFailure(failure: FailedWrite) {
        val previous = _failure.value
        _failure.value = failure
        consecutiveFailures++
        val retryDelay = RETRY_DELAYS[minOf(consecutiveFailures, RETRY_DELAYS.size) - 1]
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(retryDelay)
            writeRequests.trySend(Unit)
        }
        if (failure != previous) onSaveFailed(failure)
    }

    private companion object {
        const val RELOAD_ATTEMPTS = 5
        val RETRY_DELAYS = longArrayOf(2_000, 10_000, 30_000)
    }
}
