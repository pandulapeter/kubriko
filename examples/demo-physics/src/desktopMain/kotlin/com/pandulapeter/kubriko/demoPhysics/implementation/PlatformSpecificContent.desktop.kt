package com.pandulapeter.kubriko.demoPhysics.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.uiComponents.LargeButton
import kotlinx.coroutines.flow.MutableStateFlow
import kubriko.examples.demo_physics.generated.resources.Res
import kubriko.examples.demo_physics.generated.resources.close_scene_editor
import kubriko.examples.demo_physics.generated.resources.open_scene_editor

internal val isSceneEditorVisible = MutableStateFlow(false)
internal actual val sceneJson: MutableStateFlow<String>? = MutableStateFlow("")

@Composable
internal actual fun PlatformSpecificContent() {
    val isEditorVisible = isSceneEditorVisible.collectAsState()
    LargeButton(
        title = if (isEditorVisible.value) Res.string.close_scene_editor else Res.string.open_scene_editor,
        onButtonPressed = { isSceneEditorVisible.value = !isEditorVisible.value },
    )
}