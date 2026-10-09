/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation

import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The selected actor, the type selected for placement and its placement preview.
 *
 * [selectedActorRevision] changes whenever the selected actor is modified in place, so the UI showing it can refresh.
 */
internal class EditorSelection(
    scope: CoroutineScope,
    cameraPosition: StateFlow<SceneOffset>,
    private val instantiatePreview: (typeId: String) -> Editable<*>?,
) {
    private val _selectedActor = MutableStateFlow<Editable<*>?>(null)
    val selectedActor = _selectedActor.asStateFlow()
    private val _selectedActorRevision = MutableStateFlow(0)
    val selectedActorRevision = _selectedActorRevision.asStateFlow()
    val canLocateSelectedActor = combine(
        _selectedActor,
        cameraPosition,
        _selectedActorRevision,
    ) { selectedActor, cameraPosition, _ ->
        (selectedActor as? Visible)?.let { visibleActor ->
            !cameraPosition.isRoughlyAt(visibleActor.body.position)
        } ?: false
    }.stateIn(scope, SharingStarted.Eagerly, false)
    private val _selectedTypeId = MutableStateFlow<String?>(null)
    val selectedTypeId = _selectedTypeId.asStateFlow()
    var previewOverlayActor: Editable<*>? = null
        private set

    fun toggleActor(actor: Editable<*>) = _selectedActor.update { currentSelectedActor ->
        if (currentSelectedActor == actor) {
            null
        } else {
            actor
        }
    }

    fun setSelectedActor(actor: Editable<*>?) = _selectedActor.update { actor }

    fun notifySelectedActorUpdate() = _selectedActorRevision.update { it + 1 }

    fun toggleType(typeId: String?) {
        _selectedTypeId.update { currentValue -> if (currentValue == typeId) null else typeId }
        renewPreview()
    }

    /**
     * Replaces the placement preview with a fresh instance of the selected type, after the previous one was placed.
     */
    fun renewPreview() {
        previewOverlayActor = selectedTypeId.value?.let(instantiatePreview)
    }
}
