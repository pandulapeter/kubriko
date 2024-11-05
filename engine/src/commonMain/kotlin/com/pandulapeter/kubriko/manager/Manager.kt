package com.pandulapeter.kubriko.manager

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.KubrikoImpl
import kotlinx.coroutines.CoroutineScope

abstract class Manager {

    protected lateinit var scope: CoroutineScope
        private set

    internal fun initializeInternal(kubriko: Kubriko) {
        scope = kubriko as KubrikoImpl
        initialize(kubriko)
    }

    protected open fun initialize(kubriko: Kubriko) = Unit

    @Composable
    open fun modifier(): Modifier? = null

    @Composable
    open fun composition() = Unit

    open fun launch() = Unit

    open fun update(deltaTimeInMillis: Float, gameTimeNanos: Long) = Unit

    open fun dispose() = Unit
}