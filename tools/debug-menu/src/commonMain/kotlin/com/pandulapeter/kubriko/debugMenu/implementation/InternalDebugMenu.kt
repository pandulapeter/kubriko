package com.pandulapeter.kubriko.debugMenu.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.logger.Logger
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
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
    private val _isDebugOverlayEnabled = persistenceManager.boolean(
        key = "isDebugOverlayEnabled",
        defaultValue = false,
    )
    val isDebugOverlayEnabled = _isDebugOverlayEnabled.asStateFlow()
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
    private val _debugMenuKubriko = MutableStateFlow<Kubriko?>(null)
    val debugMenuKubriko = _debugMenuKubriko.asStateFlow()
    private val _metadata = MutableStateFlow<DebugMenuMetadata?>(null)
    val metadata = _metadata.asStateFlow()

    fun toggleVisibility() {
        _isVisible.value = !isVisible.value
    }

    fun onIsDebugOverlayEnabledChanged() = _isDebugOverlayEnabled.update { !it }

    fun onLowPriorityToggled() = _isLowPriorityEnabled.update { !it }

    fun onMediumPriorityToggled() = _isMediumPriorityEnabled.update { !it }

    fun onHighPriorityToggled() = _isHighPriorityEnabled.update { !it }

    fun onFilterUpdated(newFilter: String) = _filter.update { newFilter }

    fun toggleIsEditingFilter() = _isEditingFilter.update { !it }

    fun setGameKubriko(kubriko: Kubriko?) = _debugMenuKubriko.update {
        kubriko?.let {
            Kubriko.newInstance(
                kubriko.get<ViewportManager>(),
                DebugMenuManager(kubriko),
                persistenceManager,
            )
        }
    }

    fun setMetadata(metadata: DebugMenuMetadata) = _metadata.update { metadata }
}