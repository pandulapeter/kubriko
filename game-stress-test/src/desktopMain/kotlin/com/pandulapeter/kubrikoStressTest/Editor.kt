package com.pandulapeter.kubrikoStressTest

import com.pandulapeter.kubriko.editor.openEditor
import com.pandulapeter.kubrikoStressTest.implementation.GameObjectRegistry
import com.pandulapeter.kubrikoStressTest.implementation.GameplayController

fun main() = openEditor(
    defaultMapFilename = GameplayController.MAP_NAME,
    editableMetadata = GameObjectRegistry.typesAvailableInEditor,
)