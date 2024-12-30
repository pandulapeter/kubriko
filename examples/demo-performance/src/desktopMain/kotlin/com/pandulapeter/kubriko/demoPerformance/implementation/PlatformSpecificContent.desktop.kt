package com.pandulapeter.kubriko.demoPerformance.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.shared.ui.LargeButton
import kotlinx.coroutines.flow.MutableStateFlow
import kubriko.examples.demo_performance.generated.resources.Res
import kubriko.examples.demo_performance.generated.resources.close_scene_editor
import kubriko.examples.demo_performance.generated.resources.open_scene_editor

internal val isSceneEditorVisible = MutableStateFlow(false)
internal actual val sceneJson: MutableStateFlow<String>? = MutableStateFlow("")

@Composable
internal actual fun PlatformSpecificContent() {
    val isEditorVisible = isSceneEditorVisible.collectAsState()
    LargeButton(
        onButtonPressed = { isSceneEditorVisible.value = !isEditorVisible.value },
        title = if (isEditorVisible.value) Res.string.close_scene_editor else Res.string.open_scene_editor,
    )
}