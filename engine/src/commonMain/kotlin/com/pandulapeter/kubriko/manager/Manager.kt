package com.pandulapeter.kubriko.manager

import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class Manager {

    protected var isInitialized = false
        private set
    protected lateinit var scope: CoroutineScope
        private set
    private val autoInitializingLazyProperties = mutableListOf<AutoInitializingLazy<*>>()

    internal fun getModifierInternal(layerIndex: Int?) = getModifier(layerIndex)

    protected open fun getModifier(layerIndex: Int?): Modifier? = null

    internal fun initializeInternal(kubriko: Kubriko) {
        if (!isInitialized) {
            scope = kubriko as CoroutineScope
            onInitialize(kubriko)
            autoInitializingLazyProperties.forEach { it.initialize() }
            autoInitializingLazyProperties.clear()
            isInitialized = true
        }
    }

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
}