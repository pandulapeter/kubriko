package com.pandulapeter.kubrikoPerformanceTest

import com.pandulapeter.kubriko.sceneEditor.openSceneEditor
import com.pandulapeter.kubrikoPerformanceTest.implementation.GameplayManager
import com.pandulapeter.kubrikoPerformanceTest.implementation.KubrikoWrapper

fun main() = openSceneEditor(
    defaultMapFilename = GameplayManager.SCENE_NAME,
    serializationManager = KubrikoWrapper().serializationManager,
)