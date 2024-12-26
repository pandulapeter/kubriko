package com.pandulapeter.kubriko.manager

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class Manager {

    protected var isInitialized = false
        private set
    protected lateinit var scope: CoroutineScope
        private set
    private val autoInitializingLazyProperties = mutableListOf<AutoInitializingLazy<*>>()
    private val autoInitializingLazyManagers = mutableListOf<LazyManager<*>>()

    @Composable
    internal fun getOverlayModifierInternal() = getOverlayModifier()

    @Composable
    protected open fun getOverlayModifier(): Modifier? = null

    @Composable
    internal fun getModifierInternal(layerIndex: Int?) = getModifier(layerIndex)

    @Composable
    protected open fun getModifier(layerIndex: Int?): Modifier? = null

    internal fun initializeInternal(kubriko: Kubriko) {
        if (!isInitialized) {
            scope = kubriko as CoroutineScope
            autoInitializingLazyManagers.forEach { it.initialize(kubriko) }
            autoInitializingLazyManagers.clear()
            onInitialize(kubriko)
            autoInitializingLazyProperties.forEach { it.initialize() }
            autoInitializingLazyProperties.clear()
            isInitialized = true
        }
    }

    @Composable
    internal fun ComposableInternal() = Composable()

    @Composable
    protected open fun Composable() = Unit

    protected open fun onInitialize(kubriko: Kubriko) = Unit

    internal fun onUpdateInternal(deltaTimeInMillis: Float, gameTimeNanos: Long) = onUpdate(deltaTimeInMillis, gameTimeNanos)

    protected open fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) = Unit

    internal fun onDisposeInternal() {
        if (isInitialized) {
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