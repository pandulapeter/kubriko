package com.pandulapeter.kubrikoShowcase

import com.pandulapeter.kubriko.sceneEditor.openSceneEditor
import com.pandulapeter.kubrikoShowcase.implementation.performance.PerformanceShowcaseKubrikoWrapper
import com.pandulapeter.kubrikoShowcase.implementation.performance.PerformanceShowcaseManager

fun main() = openSceneEditor(
    defaultMapFilename = PerformanceShowcaseManager.SCENE_NAME,
    serializationManager = PerformanceShowcaseKubrikoWrapper().serializationManager,
)