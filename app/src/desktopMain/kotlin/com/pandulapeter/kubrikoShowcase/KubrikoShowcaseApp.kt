package com.pandulapeter.kubrikoShowcase

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode
import com.pandulapeter.kubrikoShowcase.implementation.performance.PerformanceShowcaseKubrikoWrapper
import com.pandulapeter.kubrikoShowcase.implementation.performance.PerformanceShowcaseManager
import com.pandulapeter.kubrikoShowcase.implementation.performance._sceneEditorRealtimeContent
import com.pandulapeter.kubrikoShowcase.implementation.performance.isSceneEditorVisible
import kotlinx.coroutines.launch
import kubriko.app.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.awt.Dimension

@OptIn(ExperimentalResourceApi::class)
fun main() = application {
    val shouldShowSceneEditorWindow = isSceneEditorVisible.collectAsState()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kubriko",
    ) {
        window.minimumSize = Dimension(400, 400)
        KubrikoShowcase()
    }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            _sceneEditorRealtimeContent.value = Res.readBytes("files/scenes/${PerformanceShowcaseManager.SCENE_NAME}.json").decodeToString()
        }
    }
    if (shouldShowSceneEditorWindow.value) {
        SceneEditor(
            defaultMapFilename = PerformanceShowcaseManager.SCENE_NAME,
            serializationManager = PerformanceShowcaseKubrikoWrapper().serializationManager,
            onCloseRequest = { isSceneEditorVisible.value = false },
            sceneEditorMode = SceneEditorMode.Connected(
                sceneJson = _sceneEditorRealtimeContent.value,
                onSceneJsonChanged = { _sceneEditorRealtimeContent.value = it },
            ),
        )
    }
}