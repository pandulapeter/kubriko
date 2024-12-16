package com.pandulapeter.kubriko.demoPerformance.implementation

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shared.ui.LargeButton
import kotlinx.coroutines.flow.MutableStateFlow
import kubriko.examples.demo_performance.generated.resources.Res
import kubriko.examples.demo_performance.generated.resources.close_scene_editor
import kubriko.examples.demo_performance.generated.resources.open_scene_editor

internal val isSceneEditorVisible = MutableStateFlow(false)
internal actual val sceneJson: MutableStateFlow<String>? = MutableStateFlow("")

@Composable
internal actual fun BoxScope.PlatformSpecificContent() {
    val isEditorVisible = isSceneEditorVisible.collectAsState()
    LargeButton(
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        onButtonPressed = { isSceneEditorVisible.value = !isEditorVisible.value },
        title = if (isEditorVisible.value) Res.string.close_scene_editor else Res.string.open_scene_editor,
    )
}