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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeRepository
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.withManagerRenamed
import com.pandulapeter.kubrikoCreator.actors.withOtherActorTypeRenamed
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * The block-built Actor types and Manager types of a project that opted in to them, and the one being edited.
 *
 * Changes show up immediately; writing them (and regenerating their code) happens in the background, one at a time
 * and in order. After each write, [syncBuildFiles] brings the project's Kubriko dependencies in line with the code.
 *
 * @param onSaved Called after each write, with whether the types changed, rather than only the Scenes they were
 *   regenerated for.
 */
internal class BlockTypesController(
    private val repository: BlockTypeRepository,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val onSaved: (haveTypesChanged: Boolean) -> Unit,
    private val onSaveFailed: () -> Unit,
    private val syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
    private val onBuildFilesSynced: (KubrikoBuildFileChanges) -> Unit = {},
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
    private val writes = Mutex()
    private val pendingWriteCount = AtomicInteger(0)

    init {
        scope.launch {
            setTypes(withContext(ioDispatcher) { repository.load() })
        }
        // The generated code of a project last opened in an older Kubriko Creator is brought up to date right away.
        write { repository.regenerateAll() }
    }

    fun select(name: String?) {
        _selectedName.value = name
    }

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
        setTypes(_types.value.filter { it.name != definition.name } + definition)
        write { repository.save(definition) }
    }

    @EditorMutation
    fun delete(name: String) {
        setTypes(_types.value.filter { it.name != name })
        if (_selectedName.value == name) _selectedName.value = null
        write { repository.delete(name) }
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
        renamed.forEach { definition -> write { repository.save(definition) } }
        write { repository.delete(oldName) }
    }

    /**
     * Throws away the changes made by hand to [name]'s generated code, so that Kubriko Creator updates it again.
     */
    @EditorMutation
    fun discardChangesMadeByHand(name: String) = write { repository.regenerateAll(overwriting = setOf(name)) }

    /**
     * Brings the generated code in line with the project's Scenes after they changed, since a Scene's music decides
     * whether the game plays audio.
     */
    fun onScenesChanged() = write(haveTypesChanged = false) { repository.regenerateAll() }

    /**
     * Reads the definitions again after they changed outside Kubriko Creator (in IntelliJ, or through git), unless one
     * of Creator's own changes is still being written, which the next change on disk is then checked against.
     */
    fun reloadFromDisk() {
        if (pendingWriteCount.get() > 0) return
        scope.launch {
            val stored = withContext(ioDispatcher) { repository.load() }
            if (pendingWriteCount.get() == 0 && stored != _types.value) {
                setTypes(stored)
                if (_selectedName.value !in stored.map { it.name }) _selectedName.value = null
                write { repository.regenerateAll() }
            }
        }
    }

    private fun setTypes(types: List<BlockTypeDefinition>) {
        val sorted = types.sortedBy { it.name }
        _types.value = sorted
        _actorTypes.value = sorted.filterIsInstance<ActorTypeDefinition>()
        _managerTypes.value = sorted.filterIsInstance<ManagerTypeDefinition>()
    }

    private fun write(haveTypesChanged: Boolean = true, change: () -> Set<String>) {
        pendingWriteCount.incrementAndGet()
        scope.launch {
            var buildFileChanges: KubrikoBuildFileChanges? = null
            val changedByHand = try {
                writes.withLock {
                    withContext(ioDispatcher) {
                        try {
                            change().also { buildFileChanges = syncBuildFiles() }
                        } catch (_: IOException) {
                            null
                        }
                    }
                }
            } finally {
                pendingWriteCount.decrementAndGet()
            }
            if (changedByHand == null) {
                onSaveFailed()
            } else {
                _changedByHand.value = changedByHand
                buildFileChanges?.let(onBuildFilesSynced)
                onSaved(haveTypesChanged)
            }
        }
    }
}
