package com.pandulapeter.gameTemplate.gameStressTest

import com.pandulapeter.gameTemplate.editor.openEditor
import com.pandulapeter.gameTemplate.gameStressTest.implementation.GameObjectRegistry
import com.pandulapeter.gameTemplate.gameStressTest.implementation.GameplayController

fun main() = openEditor(
    defaultMapFilename = GameplayController.MAP_NAME,
    supportedGameObjectSerializers = GameObjectRegistry.entries
)