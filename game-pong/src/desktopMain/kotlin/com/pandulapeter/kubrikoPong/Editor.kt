package com.pandulapeter.kubrikoPong

import com.pandulapeter.kubriko.editor.openEditor
import com.pandulapeter.kubrikoPong.implementation.GameObjectRegistry
import com.pandulapeter.kubrikoPong.implementation.GameplayController

fun main() = openEditor(
    defaultMapFilename = GameplayController.MAP_NAME,
    editableActorMetadata = GameObjectRegistry.typesAvailableInEditor
)