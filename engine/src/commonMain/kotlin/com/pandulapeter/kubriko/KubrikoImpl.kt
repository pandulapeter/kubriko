/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko

import com.pandulapeter.kubriko.logger.Logger
import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ActorManagerImpl
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.MetadataManagerImpl
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.StateManagerImpl
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.manager.ViewportManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.reflect.KClass

internal class KubrikoImpl(
    vararg manager: Manager,
    internal val tickSource: TickSource,
    override val isLoggingEnabled: Boolean,
    private val instanceNameForLogging: String?,
) : Kubriko, CoroutineScope {

    override val instanceName = instanceNameForLogging ?: toString().substringAfterLast('@')
    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val managers: List<Manager> = buildList {
        addAll(manager.distinctBy { it::class })
        if (none { it is ActorManager }) add(
            0, ActorManager.newInstance(
                isLoggingEnabled = isLoggingEnabled,
                instanceNameForLogging = instanceNameForLogging,
            )
        )
        if (none { it is MetadataManager }) add(
            0, MetadataManager.newInstance(
                isLoggingEnabled = isLoggingEnabled,
                instanceNameForLogging = instanceNameForLogging,
            )
        )
        if (none { it is StateManager }) add(
            0, StateManager.newInstance(
                isLoggingEnabled = isLoggingEnabled,
                instanceNameForLogging = instanceNameForLogging,
            )
        )
        if (none { it is ViewportManager }) add(
            0, ViewportManager.newInstance(
                isLoggingEnabled = isLoggingEnabled,
                instanceNameForLogging = instanceNameForLogging,
            )
        )
    }
    private val managerCache = mutableMapOf<KClass<out Manager>, Manager>()
    val actorManager = requireAndVerify<ActorManager, ActorManagerImpl>("ActorManager")
    val metadataManager = requireAndVerify<MetadataManager, MetadataManagerImpl>("MetadataManager")
    val stateManager = requireAndVerify<StateManager, StateManagerImpl>("StateManager")
    val viewportManager = requireAndVerify<ViewportManager, ViewportManagerImpl>("ViewportManager")

    init {
        // Internal Managers must be initialized first so that custom ones could get a stable state
        actorManager.initializeInternal(this)
        metadataManager.initializeInternal(this)
        stateManager.initializeInternal(this)
        viewportManager.initializeInternal(this)
        log(
            message = "Kubriko instance created with ${managers.size} Managers.",
            details = managers.joinToString { it::class.simpleName.orEmpty() },
            importance = Logger.Importance.HIGH,
        )
    }

    private var isInitialized = false
    private var isDisposed = false

    override fun initialize() {
        if (isDisposed) {
            throw IllegalStateException("Cannot initialize a disposed Kubriko instance. Create a new instance instead.")
        }
        if (!isInitialized) {
            log("Initializing Manager instances...")
            managers.forEach { it.initializeInternal(this) }
            if (stateManager.shouldAutoStart) {
                stateManager.updateIsRunning(true)
            }
            tickSource.initializeInternal(this)
            isInitialized = true
            log("Initialized.")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Manager, TI : T> requireAndVerify(name: String): TI {
        return get(T::class) as? TI
            ?: throw IllegalStateException("Custom implementations of the $name interface are not supported. Use $name.newInstance() to instantiate $name.")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Manager> get(managerType: KClass<T>): T {
        if (isDisposed) {
            throw IllegalStateException("Cannot access Managers on a disposed Kubriko instance.")
        }
        val cached = managerCache[managerType] as? T
        if (cached != null) return cached
        val found = managers.firstOrNull { managerType.isInstance(it) } as? T
            ?: throw IllegalStateException("${managerType.simpleName} has not been registered in Kubriko.newInstance().")
        managerCache[managerType] = found
        return found
    }

    override fun dispose() {
        if (isDisposed) return
        log("Disposing Manager instances...")
        tickSource.onDisposeInternal()
        managers.forEach { it.onDisposeInternal() }
        cancel()
        managerCache.clear()
        isInitialized = false
        isDisposed = true
        log(
            message = "Disposed.",
            importance = Logger.Importance.HIGH,
        )
    }

    private fun log(
        message: String,
        details: String? = null,
        importance: Logger.Importance = Logger.Importance.LOW,
    ) {
        if (isLoggingEnabled) {
            Logger.log(
                message = message,
                details = details,
                source = "Kubriko@$instanceName",
                importance = importance,
            )
        }
    }

    internal fun onTick(deltaTimeInMilliseconds: Int) {
        managers.forEach { it.onUpdateInternal(deltaTimeInMilliseconds) }
    }
}
