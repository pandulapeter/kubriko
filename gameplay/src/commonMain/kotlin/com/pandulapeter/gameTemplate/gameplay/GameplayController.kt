package com.pandulapeter.gameTemplate.gameplay

import com.pandulapeter.gameTemplate.gameplay.implementation.GameplayControllerImpl
import com.pandulapeter.gameTemplate.gameplay.models.Metadata
import kotlinx.coroutines.flow.StateFlow

interface GameplayController {

    val isRunning: StateFlow<Boolean>
    val metadata: StateFlow<Metadata>

    fun start()

    fun updateIsRunning(isRunning: Boolean)

    companion object {
        fun get(): GameplayController = GameplayControllerImpl
    }
}
