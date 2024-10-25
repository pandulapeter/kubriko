package com.pandulapeter.gameTemplate.gameplayController

import com.pandulapeter.gameTemplate.gameplayController.implementation.GameplayControllerImpl
import com.pandulapeter.gameTemplate.gameplayController.models.Metadata
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
