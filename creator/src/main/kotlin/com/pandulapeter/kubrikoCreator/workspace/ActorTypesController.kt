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
import com.pandulapeter.kubrikoCreator.actors.ActorTypeNaming
import com.pandulapeter.kubrikoCreator.actors.ActorTypeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * The block-built Actor types of a project that opted in to them, and the one being edited.
 *
 * Changes show up immediately; writing them (and regenerating their code) happens in the background, one at a time
 * and in order.
 */
internal class ActorTypesController(
    private val repository: ActorTypeRepository,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val onSaved: () -> Unit,
    private val onSaveFailed: () -> Unit,
) {
    private val _actorTypes = MutableStateFlow(emptyList<ActorTypeDefinition>())
    val actorTypes = _actorTypes.asStateFlow()
    private val _selectedName = MutableStateFlow<String?>(null)
    val selectedName = _selectedName.asStateFlow()
    private val _changedByHand = MutableStateFlow(emptySet<String>())

    /**
     * The Actor types whose generated code was changed by hand (in IntelliJ, for example), and which Kubriko Creator
     * therefore stopped updating until those changes are discarded ([discardChangesMadeByHand]).
     */
    val changedByHand = _changedByHand.asStateFlow()
    private val writes = Mutex()
    private val pendingWriteCount = AtomicInteger(0)

    init {
        scope.launch {
            _actorTypes.value = withContext(ioDispatcher) { repository.load() }
        }
        // The generated code of a project last opened in an older Kubriko Creator is brought up to date right away.
        write { repository.regenerateAll() }
    }

    fun select(name: String?) {
        _selectedName.value = name
    }

    /**
     * Creates an empty Actor type and selects it.
     *
     * @return Why [name] can't be used, or null if the Actor type was created.
     */
    fun create(name: String): StringResource? {
        ActorTypeNaming.validate(name, _actorTypes.value.map { it.name })?.let { return it }
        update(ActorTypeDefinition(name = name))
        _selectedName.value = name
        return null
    }

    /**
     * Replaces the Actor type of the same name.
     */
    fun update(definition: ActorTypeDefinition) {
        _actorTypes.update { actorTypes -> (actorTypes.filter { it.name != definition.name } + definition).sortedBy { it.name } }
        write { repository.save(definition) }
    }

    fun delete(name: String) {
        _actorTypes.update { actorTypes -> actorTypes.filter { it.name != name } }
        if (_selectedName.value == name) _selectedName.value = null
        write { repository.delete(name) }
    }

    /**
     * Throws away the changes made by hand to [name]'s generated code, so that Kubriko Creator updates it again.
     */
    fun discardChangesMadeByHand(name: String) = write { repository.regenerateAll(overwriting = setOf(name)) }

    /**
     * Reads the definitions again after they changed outside Kubriko Creator (in IntelliJ, or through git), unless one
     * of Creator's own changes is still being written, which the next change on disk is then checked against.
     */
    fun reloadFromDisk() {
        if (pendingWriteCount.get() > 0) return
        scope.launch {
            val stored = withContext(ioDispatcher) { repository.load() }
            if (pendingWriteCount.get() == 0 && stored != _actorTypes.value) {
                _actorTypes.value = stored
                if (_selectedName.value !in stored.map { it.name }) _selectedName.value = null
                write { repository.regenerateAll() }
            }
        }
    }

    private fun write(change: () -> Set<String>) {
        pendingWriteCount.incrementAndGet()
        scope.launch {
            val changedByHand = try {
                writes.withLock {
                    withContext(ioDispatcher) {
                        try {
                            change()
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
                onSaved()
            }
        }
    }
}
