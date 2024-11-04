package com.pandulapeter.kubrikoStressTest

import com.pandulapeter.kubriko.sceneEditor.openSceneEditor
import com.pandulapeter.kubrikoStressTest.implementation.GameplayController
import com.pandulapeter.kubrikoStressTest.implementation.SceneSerializerWrapper

fun main() = openSceneEditor(
    defaultMapFilename = GameplayController.SCENE_NAME,
    actorSerializer = SceneSerializerWrapper().sceneSerializer,
)