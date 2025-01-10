package com.pandulapeter.kubriko.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class Manager(
    initialIsLoggingEnabled: Boolean = false,
) {

    private val _isInitialized = MutableStateFlow(false)
    protected val isInitialized = _isInitialized.asStateFlow()
    protected lateinit var scope: CoroutineScope
        private set
    private val autoInitializingLazyProperties = mutableListOf<AutoInitializingLazy<*>>()
    private val autoInitializingLazyManagers = mutableListOf<LazyManager<*>>()
    protected var isLoggingEnabled = initialIsLoggingEnabled

    @Composable
    internal fun processOverlayModifierInternal(modifier: Modifier) = processOverlayModifier(modifier)

    @Composable
    protected open fun processOverlayModifier(modifier: Modifier): Modifier = modifier

    @Composable
    internal fun processModifierInternal(modifier: Modifier, layerIndex: Int?) = processModifier(modifier, layerIndex)

    @Composable
    protected open fun processModifier(modifier: Modifier, layerIndex: Int?): Modifier = modifier

    internal fun initializeInternal(kubriko: Kubriko) {
        if (!isInitialized.value) {
            scope = kubriko as CoroutineScope
            autoInitializingLazyManagers.forEach { it.initialize(kubriko) }
            autoInitializingLazyManagers.clear()
            _isInitialized.value = true
            onInitialize(kubriko)
            autoInitializingLazyProperties.forEach { it.initialize() }
            autoInitializingLazyProperties.clear()
        }
    }

    @Composable
    internal fun ComposableInternal(insetPaddingModifier: Modifier) {
        if (isInitialized.collectAsState().value) {
            Composable(insetPaddingModifier)
        }
    }

    @Composable
    protected open fun Composable(insetPaddingModifier: Modifier) = Unit

    protected open fun onInitialize(kubriko: Kubriko) = Unit

    internal fun onUpdateInternal(deltaTimeInMilliseconds: Float, gameTimeMilliseconds: Long) = onUpdate(deltaTimeInMilliseconds, gameTimeMilliseconds)

    protected open fun onUpdate(deltaTimeInMilliseconds: Float, gameTimeMilliseconds: Long) = Unit

    internal fun onDisposeInternal() {
        if (isInitialized.value) {
            _isInitialized.update { false }
            onDispose()
        }
    }

    protected open fun onDispose() = Unit

    protected fun <T> autoInitializingLazy(initializer: () -> T): ReadOnlyProperty<Manager, T> = AutoInitializingLazy(initializer)

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

    protected inline fun <reified T : Manager> manager(): ReadOnlyProperty<Manager, T> = manager(T::class)

    protected fun <T : Manager> manager(managerType: KClass<T>): ReadOnlyProperty<Manager, T> = LazyManager(managerType)

    private fun log(
        message: String,
        details: String? = null,
    ) {
        if (isLoggingEnabled) {
            Logger.log(
                message = message,
                details = details,
                source = "Manager_$this" // TODO
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

        override fun getValue(thisRef: Manager, property: KProperty<*>) = value
    }
}