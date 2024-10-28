package com.pandulapeter.gameTemplate.gameStressTest

import com.pandulapeter.gameTemplate.gameStressTest.implementation.GameplayControllerImpl
import com.pandulapeter.gameTemplate.gameStressTest.models.Metadata
import kotlinx.coroutines.flow.StateFlow

interface GameplayController {

    val metadata: StateFlow<Metadata>

    companion object {
        fun get(): GameplayController = GameplayControllerImpl
    }
}
