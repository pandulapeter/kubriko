package com.pandulapeter.gameTemplate.gameStressTest

import com.pandulapeter.gameTemplate.editor.openEditor

fun main() = openEditor(
    supportedGameObjectSerializers = GameObjectRegistry.entries
)