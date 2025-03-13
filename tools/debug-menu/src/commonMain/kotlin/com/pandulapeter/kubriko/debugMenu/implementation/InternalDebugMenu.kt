/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.logger.Logger
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

internal object InternalDebugMenu {

    private val persistenceManager = PersistenceManager.newInstance(fileName = "kubrikoDebugMenu")
    private val _isVisible = persistenceManager.boolean(
        key = "isVisible",
        defaultValue = false,
    )
    val isVisible = _isVisible.asStateFlow()
    private val _shouldDrawBodyOverlays = persistenceManager.boolean(
        key = "shouldDrawBodyOverlays",
        defaultValue = false,
    )
    val shouldDrawBodyOverlays = _shouldDrawBodyOverlays.asStateFlow()
    private val _shouldDrawCollisionMaskOverlays = persistenceManager.boolean(
        key = "shouldDrawCollisionMaskOverlays",
        defaultValue = false,
    )
    val shouldDrawCollisionMaskOverlays = _shouldDrawCollisionMaskOverlays.asStateFlow()
    private val _isLowPriorityEnabled = persistenceManager.boolean(
        key = "isLowPriorityEnabled",
        defaultValue = true,
    )
    val isLowPriorityEnabled = _isLowPriorityEnabled.asStateFlow()
    private val _isMediumPriorityEnabled = persistenceManager.boolean(
        key = "isMediumPriorityEnabled",
        defaultValue = true,
    )
    val isMediumPriorityEnabled = _isMediumPriorityEnabled.asStateFlow()
    private val _isHighPriorityEnabled = persistenceManager.boolean(
        key = "isHighPriorityEnabled",
        defaultValue = true,
    )
    val isHighPriorityEnabled = _isHighPriorityEnabled.asStateFlow()
    private val _filter = persistenceManager.string(
        key = "filter",
        defaultValue = "",
    )
    val filter = _filter.asStateFlow()
    private val _isEditingFilter = MutableStateFlow(false)
    val isEditingFilter = _isEditingFilter.asStateFlow()
    val logs = combine(
        Logger.logs,
        isLowPriorityEnabled,
        isMediumPriorityEnabled,
        isHighPriorityEnabled,
        filter,
    ) { logs,
        isLowPriorityEnabled,
        isMediumPriorityEnabled,
        isHighPriorityEnabled,
        filter ->
        logs.filter {
            when (it.importance) {
                Logger.Importance.LOW -> isLowPriorityEnabled
                Logger.Importance.MEDIUM -> isMediumPriorityEnabled
                Logger.Importance.HIGH -> isHighPriorityEnabled
            }
        }.filter { it.source?.contains(filter, true) == true || it.message.contains(filter, true) }
    }
    private val _debugMenuKubriko = MutableStateFlow(persistentMapOf<String, Kubriko>())
    val debugMenuKubriko = _debugMenuKubriko.asStateFlow()
    private val _metadata = MutableStateFlow<DebugMenuMetadata?>(null)
    val metadata = _metadata.asStateFlow()
    val internalKubriko by lazy {
        Kubriko.newInstance(
            persistenceManager,
        )
    }

    fun toggleVisibility() {
        _isVisible.value = !isVisible.value
    }

    fun onIsBodyOverlayEnabledChanged() = _shouldDrawBodyOverlays.update { !it }

    fun onIsCollisionMaskOverlayEnabledChanged() = _shouldDrawCollisionMaskOverlays.update { !it }

    fun onLowPriorityToggled() = _isLowPriorityEnabled.update { !it }

    fun onMediumPriorityToggled() = _isMediumPriorityEnabled.update { !it }

    fun onHighPriorityToggled() = _isHighPriorityEnabled.update { !it }

    fun onFilterUpdated(newFilter: String) = _filter.update { newFilter }

    fun toggleIsEditingFilter() = _isEditingFilter.update { !it }

    fun setGameKubriko(kubriko: Kubriko?) {
        val mutableMap = debugMenuKubriko.value.toMutableMap()
        if (mutableMap[kubriko?.instanceName] == null && kubriko != null) {
            mutableMap[kubriko.instanceName] = Kubriko.newInstance(
                kubriko.get<ViewportManager>(),
                DebugMenuManager(kubriko),
            )
            _debugMenuKubriko.update { mutableMap.toPersistentMap() }
        }
    }

    fun clearGameKubriko(kubriko: Kubriko?) {
        val mutableMap = debugMenuKubriko.value.toMutableMap()
        mutableMap[kubriko?.instanceName]?.let { debugMenuKubriko ->
            debugMenuKubriko.dispose()
            mutableMap.remove(kubriko?.instanceName)
            _debugMenuKubriko.update { mutableMap.toPersistentMap() }
        }
    }

    fun setMetadata(metadata: DebugMenuMetadata) = _metadata.update { metadata }
}