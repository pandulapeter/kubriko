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
    private val writes = Mutex()

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

    private fun write(change: () -> Unit) {
        scope.launch {
            val isSuccessful = writes.withLock {
                withContext(ioDispatcher) {
                    try {
                        change()
                        true
                    } catch (_: IOException) {
                        false
                    }
                }
            }
            if (isSuccessful) onSaved() else onSaveFailed()
        }
    }
}
