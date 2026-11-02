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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The block-built Actor types and Manager types of a project that opted in to them, and the one being edited.
 *
 * Changes show up immediately; writing them happens in the background, in order, in batches: the changes made within
 * [writeDelayInMilliseconds] of each other (typing into a field, for example) are written together, a type saved
 * several times is written once, and the code is regenerated once per batch, from the types in memory. After each
 * batch, [syncBuildFiles] brings the project's Kubriko dependencies in line with the code.
 *
 * While a definition file can't be read, the code isn't regenerated, nor are the build files synced, since code generated
 * without that type would break the build; the file is never written over or deleted. Neither happens while [isReadOnly]
 * either: the project was saved by a newer Kubriko Creator, whose code this one would generate wrongly.
 *
 * @param onSaved Called after each batch, with whether the types changed, rather than only the Scenes they were
 *   regenerated for.
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
    private val onSaveFailed: () -> Unit,
    private val onFileUnreadable: (UnreadableBlockTypeFile) -> Unit = {},
    private val onFirstLoaded: (List<BlockTypeDefinition>) -> Unit = {},
    private val onFilesReadableAgain: () -> Unit = {},
    private val onLeftoversRemoved: (List<String>) -> Unit = {},
    private val onLeftoverChangedByHand: (String) -> Unit = {},
    private val syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
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
    private val pendingWrites = ArrayList<PendingWrite>()
    private var havePendingTypesChanged = false
    private var isReloadRequested = false
    private val writeRequests = Channel<Unit>(Channel.CONFLATED)
    private val pendingWriteCount = MutableStateFlow(0)

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
     * Returns once every change made so far is written.
     */
    suspend fun awaitWrites() {
        pendingWriteCount.first { it == 0 }
    }

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
     * of Creator's own changes is still being written, in which case they are read once it is.
     */
    fun reloadFromDisk() {
        if (pendingWriteCount.value > 0) {
            isReloadRequested = true
            return
        }
        scope.launch { reload() }
    }

    /**
     * Reads the definitions again, as [reloadFromDisk] does, returning once they are in memory.
     */
    suspend fun reload() {
        if (pendingWriteCount.value > 0) {
            isReloadRequested = true
            return
        }
        val (loaded, storedFolders) = try {
            withContext(ioDispatcher) { repository.load() to BlockTypeKind.entries.associateWith(repository::folders) }
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            return
        }
        if (pendingWriteCount.value > 0) {
            isReloadRequested = true
            return
        }
        if (loaded.types != _types.value || storedFolders != emptyFolders || loaded.unreadableFiles != _unreadableFiles.value) {
            emptyFolders = storedFolders
            setUnreadableFiles(loaded.unreadableFiles)
            setTypes(loaded.types)
            if (_selectedName.value !in loaded.types.map { it.name }) _selectedName.value = null
            queue(PendingWrite.Regenerate)
        }
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
        pendingWriteCount.update { it + 1 }
        writeRequests.trySend(Unit)
    }

    /**
     * Writes every change queued so far as one batch, then regenerates the code from the types as they are in memory.
     */
    private suspend fun writePending() {
        if (pendingWrites.isEmpty()) return
        val batch = pendingWrites.toList()
        pendingWrites.clear()
        val haveTypesChanged = havePendingTypesChanged
        havePendingTypesChanged = false
        var buildFileChanges: KubrikoBuildFileChanges? = null
        val regenerated = try {
            val types = _types.value
            val canRegenerate = _unreadableFiles.value.isEmpty() && !isReadOnly()
            withContext(ioDispatcher) {
                try {
                    PendingWrite.withoutOverwrittenSaves(batch).forEach { write ->
                        when (write) {
                            is PendingWrite.Save -> repository.save(write.definition)
                            is PendingWrite.Delete -> repository.delete(write.name)
                            is PendingWrite.CreateFolder -> repository.createFolder(write.kind, write.path)
                            is PendingWrite.DeleteFolder -> repository.deleteFolder(write.kind, write.path)
                            PendingWrite.Regenerate, is PendingWrite.Overwrite -> Unit
                        }
                    }
                    if (canRegenerate) {
                        repository.regenerate(types, overwriting = batch.filterIsInstance<PendingWrite.Overwrite>().mapTo(mutableSetOf()) { it.name })
                            .also { buildFileChanges = syncBuildFiles() }
                    } else {
                        RegeneratedCode(changedByHand = _changedByHand.value, leftoversChangedByHand = leftoversChangedByHand)
                    }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    null
                }
            }
        } finally {
            pendingWriteCount.update { it - batch.size }
        }
        try {
            if (regenerated == null) {
                onSaveFailed()
            } else {
                _changedByHand.value = regenerated.changedByHand
                if (regenerated.removedLeftovers.isNotEmpty()) onLeftoversRemoved(regenerated.removedLeftovers)
                regenerated.leftoversChangedByHand.filter { it !in leftoversChangedByHand }.sorted().forEach(onLeftoverChangedByHand)
                leftoversChangedByHand = regenerated.leftoversChangedByHand
                buildFileChanges?.let(onBuildFilesSynced)
                onSaved(haveTypesChanged)
            }
        } finally {
            if (isReloadRequested && pendingWriteCount.value == 0) {
                isReloadRequested = false
                scope.launch { reload() }
            }
        }
    }
}
