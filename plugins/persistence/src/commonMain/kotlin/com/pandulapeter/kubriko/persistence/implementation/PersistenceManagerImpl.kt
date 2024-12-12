package com.pandulapeter.kubriko.persistence.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.persistence.PersistenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class PersistenceManagerImpl(
    fileName: String,
) : PersistenceManager() {
    private val keyValuePersistenceManager by lazy { createKeyValuePersistenceManager(fileName = fileName) }
    private val stateFlowMap = mutableMapOf<String, PersistedPropertyWrapper<*>>()

    override fun onInitialize(kubriko: Kubriko) {
        stateFlowMap.values.forEach { wrapper ->
            wrapper.load(keyValuePersistenceManager)
            wrapper.flow.onEach { wrapper.save(keyValuePersistenceManager) }.launchIn(scope)
        }
    }

    override fun boolean(key: String, defaultValue: Boolean) = (
            stateFlowMap[key] as? PersistedPropertyWrapper.Boolean ?: PersistedPropertyWrapper.Boolean(key, defaultValue).also { wrapper ->
                if (isInitialized) {
                    wrapper.load(keyValuePersistenceManager)
                    wrapper.flow.onEach { wrapper.save(keyValuePersistenceManager) }.launchIn(scope)
                }
                stateFlowMap[key] = wrapper
            }
            ).flow

    override fun int(key: String, defaultValue: Int) = (
            stateFlowMap[key] as? PersistedPropertyWrapper.Int ?: PersistedPropertyWrapper.Int(key, defaultValue).also { wrapper ->
                if (isInitialized) {
                    wrapper.load(keyValuePersistenceManager)
                    wrapper.flow.onEach { wrapper.save(keyValuePersistenceManager) }.launchIn(scope)
                }
                stateFlowMap[key] = wrapper
            }
            ).flow

    override fun string(key: String, defaultValue: String) = (
            stateFlowMap[key] as? PersistedPropertyWrapper.String ?: PersistedPropertyWrapper.String(key, defaultValue).also { wrapper ->
                if (isInitialized) {
                    wrapper.load(keyValuePersistenceManager)
                    wrapper.flow.onEach { wrapper.save(keyValuePersistenceManager) }.launchIn(scope)
                }
                stateFlowMap[key] = wrapper
            }
            ).flow

    @Suppress("UNCHECKED_CAST")
    override fun <T> generic(key: String, defaultValue: T, serializer: (T) -> String, deserializer: (String) -> T) = (
            stateFlowMap[key] as? PersistedPropertyWrapper.Generic ?: PersistedPropertyWrapper.Generic(key, defaultValue, serializer, deserializer).also { wrapper ->
                if (isInitialized) {
                    wrapper.load(keyValuePersistenceManager)
                    wrapper.flow.onEach { wrapper.save(keyValuePersistenceManager) }.launchIn(scope)
                }
                stateFlowMap[key] = wrapper
            }
            ).flow as MutableStateFlow<T>
}