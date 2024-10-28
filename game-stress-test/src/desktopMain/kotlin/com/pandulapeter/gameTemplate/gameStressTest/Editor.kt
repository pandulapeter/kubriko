package com.pandulapeter.gameTemplate.gameStressTest

import com.pandulapeter.gameTemplate.editor.openEditor
import com.pandulapeter.gameTemplate.gameStressTest.implementation.GameplayControllerImpl

fun main() = openEditor(
    defaultMapFilename = GameplayControllerImpl.MAP_NAME,
    supportedGameObjectSerializers = GameObjectRegistry.entries
)