package com.pandulapeter.kubriko.persistence

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.persistence.implementation.KeyValuePersistenceManager
import com.pandulapeter.kubriko.persistence.implementation.PersistedPropertyWrapper
import com.pandulapeter.kubriko.persistence.implementation.createKeyValuePersistenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class PersistenceManagerImpl(
    private val fileName: String,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : PersistenceManager(isLoggingEnabled, instanceNameForLogging) {
    private var keyValuePersistenceManager: KeyValuePersistenceManager? = null
    private val stateFlowMap = mutableMapOf<String, PersistedPropertyWrapper<*>>()

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        if (keyValuePersistenceManager == null && isInitialized.value) {
            keyValuePersistenceManager = createKeyValuePersistenceManager(fileName = fileName).also { keyValuePersistenceManager ->
                scope.launch(Dispatchers.Default) {
                    stateFlowMap.values.forEach { wrapper ->
                        wrapper.load(keyValuePersistenceManager)
                        wrapper.flow.onEach { wrapper.save(keyValuePersistenceManager) }.launchIn(scope)
                    }
                }
            }
        }
    }

    override fun boolean(
        key: String,
        defaultValue: Boolean,
    ) = (stateFlowMap[key] as? PersistedPropertyWrapper.Boolean
        ?: PersistedPropertyWrapper.Boolean(key, defaultValue).initialize()).flow

    override fun int(
        key: String,
        defaultValue: Int,
    ) = (stateFlowMap[key] as? PersistedPropertyWrapper.Int
        ?: PersistedPropertyWrapper.Int(key, defaultValue).initialize()).flow

    override fun float(
        key: String,
        defaultValue: Float,
    ) = (stateFlowMap[key] as? PersistedPropertyWrapper.Float
        ?: PersistedPropertyWrapper.Float(key, defaultValue).initialize()).flow

    override fun string(
        key: String,
        defaultValue: String,
    ) = (stateFlowMap[key] as? PersistedPropertyWrapper.String
        ?: PersistedPropertyWrapper.String(key, defaultValue).initialize()).flow

    @Suppress("UNCHECKED_CAST")
    override fun <T> generic(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ) = (stateFlowMap[key] as? PersistedPropertyWrapper.Generic
        ?: PersistedPropertyWrapper.Generic(key, defaultValue, serializer, deserializer).initialize()).flow as MutableStateFlow<T>

    private fun <T> PersistedPropertyWrapper<T>.initialize() = this.also { wrapper ->
        keyValuePersistenceManager?.let { keyValuePersistenceManager ->
            scope.launch(Dispatchers.Default) {
                wrapper.load(keyValuePersistenceManager)
                wrapper.flow.onEach { wrapper.save(keyValuePersistenceManager) }.launchIn(scope)
            }
        }
        stateFlowMap[key] = wrapper
    }

    override fun onDispose() {
        keyValuePersistenceManager = null
        stateFlowMap.clear()
    }
}