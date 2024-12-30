package com.pandulapeter.kubriko.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class Manager {

    private val _isInitialized = MutableStateFlow(false)
    protected val isInitialized = _isInitialized.asStateFlow()
    protected lateinit var scope: CoroutineScope
        private set
    private val autoInitializingLazyProperties = mutableListOf<AutoInitializingLazy<*>>()
    private val autoInitializingLazyManagers = mutableListOf<LazyManager<*>>()

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
            onInitialize(kubriko)
            autoInitializingLazyProperties.forEach { it.initialize() }
            autoInitializingLazyProperties.clear()
            _isInitialized.value = true
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

    internal fun onUpdateInternal(deltaTimeInMillis: Float, gameTimeNanos: Long) = onUpdate(deltaTimeInMillis, gameTimeNanos)

    protected open fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) = Unit

    internal fun onDisposeInternal() {
        if (isInitialized.value) {
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