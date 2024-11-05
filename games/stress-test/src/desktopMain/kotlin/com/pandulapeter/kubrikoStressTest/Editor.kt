package com.pandulapeter.kubrikoStressTest

import com.pandulapeter.kubriko.sceneEditor.openSceneEditor
import com.pandulapeter.kubrikoStressTest.implementation.GameplayManager
import com.pandulapeter.kubrikoStressTest.implementation.KubrikoWrapper

fun main() = openSceneEditor(
    defaultMapFilename = GameplayManager.SCENE_NAME,
    serializationManager = KubrikoWrapper().serializationManager,
)