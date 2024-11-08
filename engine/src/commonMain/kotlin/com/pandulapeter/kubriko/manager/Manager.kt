package com.pandulapeter.kubriko.manager

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

abstract class Manager {

    protected var isInitialized = false
        private set
    protected lateinit var scope: CoroutineScope
        private set

    open fun getModifier(canvasIndex: Int?): Flow<Modifier>? = null

    internal fun initializeInternal(kubriko: Kubriko) {
        if (!isInitialized) {
            scope = kubriko as CoroutineScope
            onInitialize(kubriko)
            isInitialized = true
        }
    }

    protected open fun onInitialize(kubriko: Kubriko) = Unit

    @Composable
    open fun onRecomposition() = Unit

    open fun onLaunch() = Unit

    open fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) = Unit

    open fun onDispose() = Unit
}