package com.pandulapeter.kubrikoStressTest

import com.pandulapeter.kubriko.sceneEditor.openSceneEditor
import com.pandulapeter.kubrikoStressTest.implementation.GameObjectRegistry
import com.pandulapeter.kubrikoStressTest.implementation.GameplayController

fun main() = openSceneEditor(
    defaultMapFilename = GameplayController.SCENE_NAME,
    editableMetadata = GameObjectRegistry.typesAvailableInEditor,
)