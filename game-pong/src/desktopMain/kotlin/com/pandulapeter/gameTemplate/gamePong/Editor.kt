package com.pandulapeter.gameTemplate.gamePong

import com.pandulapeter.gameTemplate.editor.openEditor
import com.pandulapeter.gameTemplate.gamePong.implementation.GameplayController

fun main() = openEditor(
    defaultMapFilename = GameplayController.MAP_NAME,
    supportedGameObjectSerializers = GameObjectRegistry.entries
)