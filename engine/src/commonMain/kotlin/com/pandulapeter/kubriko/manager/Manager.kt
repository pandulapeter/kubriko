/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Base class for all managers in the Kubriko engine.
 * Managers are responsible for handling specific aspects of the game logic and state.
 * They are added to the Kubriko instance at creation and cannot be removed later on.
 *
 * @param isLoggingEnabled Whether to enable logging for this manager.
 * @param instanceNameForLogging Optional name to use for this instance in log messages.
 * @param classNameForLogging Optional class name to use for this instance in log messages.
 */
abstract class Manager(
    protected val isLoggingEnabled: Boolean = false,
    private val instanceNameForLogging: String? = null,
    private val classNameForLogging: String? = null,
) {
    private val _isInitialized = MutableStateFlow(false)

    /**
     * Whether the manager has been initialized.
     */
    protected val isInitialized = _isInitialized.asStateFlow()
    private lateinit var _scope: CoroutineScope

    /**
     * The [CoroutineScope] of the [Kubriko] instance this manager is attached to.
     * Only available after [onInitialize] has been called.
     * Lives until the Kubriko instance is disposed.
     */
    protected val scope
        get() = try {
            _scope
        } catch (_: RuntimeException) {
            throw IllegalStateException("Cannot use the scope of ${this::class.simpleName} until the Manager has been initialized. Make sure it's added to Kubriko and the connected KubrikoViewport is visible.")
        }
    private val autoInitializingLazyProperties = mutableListOf<AutoInitializingLazy<*>>()
    private val autoInitializingLazyManagers = mutableListOf<LazyManager<*>>()

    @Composable
    internal fun processOverlayModifierInternal(modifier: Modifier) = processOverlayModifier(modifier)

    /**
     * Allows the manager to apply modifiers to the overlay layer of the viewport.
     */
    @Composable
    protected open fun processOverlayModifier(modifier: Modifier): Modifier = modifier

    @Composable
    internal fun processModifierInternal(modifier: Modifier, layerIndex: Int?, gameTime: State<Long>) = processModifier(modifier, layerIndex, gameTime)

    /**
     * Allows the manager to apply modifiers to the game world layers.
     */
    @Composable
    protected open fun processModifier(modifier: Modifier, layerIndex: Int?, gameTime: State<Long>): Modifier = modifier

    internal fun initializeInternal(kubriko: Kubriko) {
        if (!isInitialized.value) {
            log(
                message = "Initializing...",
                importance = Logger.Importance.LOW,
            )
            _scope = kubriko as CoroutineScope
            autoInitializingLazyManagers.forEach { it.initialize(kubriko) }
            autoInitializingLazyManagers.clear()
            _isInitialized.value = true
            onInitialize(kubriko)
            autoInitializingLazyProperties.forEach { it.initialize() }
            autoInitializingLazyProperties.clear()
            log(
                message = "Initialized.",
                importance = Logger.Importance.MEDIUM,
            )
        }
    }

    @Composable
    internal fun ComposableInternal(windowInsets: WindowInsets) {
        if (isInitialized.collectAsState().value) {
            Composable(windowInsets)
        }
    }

    /**
     * A Composable that is rendered within the [KubrikoViewport].
     * This can be used to draw UI elements or other overlays.
     *
     * @param windowInsets The window insets of the viewport.
     */
    @Composable
    protected open fun Composable(windowInsets: WindowInsets) = Unit

    /**
     * Called when the manager is being initialized.
     * Default managers get initialized first, after that the order of addition is preserved.
     *
     * @param kubriko The [Kubriko] instance this manager is attached to.
     */
    protected open fun onInitialize(kubriko: Kubriko) = Unit

    internal fun onUpdateInternal(deltaTimeInMilliseconds: Int) = onUpdate(deltaTimeInMilliseconds)

    /**
     * Called on every frame update of the game loop.
     *
     * @param deltaTimeInMilliseconds The time elapsed since the last frame in milliseconds.
     */
    protected open fun onUpdate(deltaTimeInMilliseconds: Int) = Unit

    internal fun onDisposeInternal() {
        if (isInitialized.value) {
            log(
                message = "Disposing...",
                importance = Logger.Importance.LOW,
            )
            _isInitialized.update { false }
            onDispose()
            log(
                message = "Disposed.",
                importance = Logger.Importance.MEDIUM,
            )
        }
    }

    /**
     * Called when the manager is being disposed.
     * Should not be called manually, the dispose() function of Kubriko triggers the disposal of all attached managers.
     * Use this to clean up any resources or subscriptions.
     */
    protected open fun onDispose() = Unit

    /**
     * A lazy property that is automatically initialized during [onInitialize].
     */
    protected fun <T> autoInitializingLazy(initializer: () -> T): ReadOnlyProperty<Manager, T> = AutoInitializingLazy(initializer)

    /**
     * Converts a [Flow] into a [StateFlow] using the manager's scope and moves it to the Main dispatcher.
     */
    protected fun <T> Flow<T>.asStateFlowOnMainThread(initialValue: T) = stateIn(scope + Dispatchers.Main, SharingStarted.Eagerly, initialValue)

    /**
     * Converts a [Flow] into a [StateFlow] using the manager's scope.
     */
    protected fun <T> Flow<T>.asStateFlow(initialValue: T) = stateIn(scope, SharingStarted.Eagerly, initialValue)

    private inner class AutoInitializingLazy<T>(initializer: () -> T) : ReadOnlyProperty<Manager, T> {

        private val value by lazy(initializer)

        init {
            autoInitializingLazyProperties.add(this)
        }

        fun initialize() {
            value
        }

        override fun getValue(thisRef: Manager, property: KProperty<*>) = value
    }

    /**
     * Retrieves another [Manager] instance.
     */
    protected inline fun <reified T : Manager> manager(): ReadOnlyProperty<Manager, T> = manager(T::class)

    /**
     * Retrieves another [Manager] instance.
     */
    protected fun <T : Manager> manager(managerType: KClass<T>): ReadOnlyProperty<Manager, T> = LazyManager(managerType)

    /**
     * Logs a message with the manager's source information.
     */
    protected fun log(
        message: String,
        details: String? = null,
        importance: Logger.Importance = Logger.Importance.HIGH,
    ) {
        if (isLoggingEnabled) {
            val className = classNameForLogging ?: this::class.simpleName
            val instanceName = instanceNameForLogging ?: toString().substringAfterLast('@')
            Logger.log(
                message = message,
                details = details,
                source = "$className@$instanceName",
                importance = importance,
            )
        }
    }


    private inner class LazyManager<T : Manager>(private val managerType: KClass<T>) : ReadOnlyProperty<Manager, T> {

        private lateinit var value: T

        init {
            autoInitializingLazyManagers.add(this)
        }

        fun initialize(kubriko: Kubriko) {
            value = kubriko.get(managerType)
        }

        override fun getValue(thisRef: Manager, property: KProperty<*>) = try {
            value
        } catch (_: RuntimeException) {
            throw IllegalStateException("${managerType.simpleName} has not been registered in Kubriko.newInstance(), or you're trying to access it before this Manager has been initialized.")
        }
    }
}