package com.pandulapeter.kubrikoPong

import com.pandulapeter.kubriko.sceneEditor.openSceneEditor
import com.pandulapeter.kubrikoPong.implementation.GameObjectRegistry
import com.pandulapeter.kubrikoPong.implementation.GameplayController

fun main() = openSceneEditor(
    defaultMapFilename = GameplayController.SCENE_NAME,
    editableMetadata = GameObjectRegistry.typesAvailableInEditor
)