package com.pandulapeter.kubrikoShowcase.implementation.performance

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.close_scene_editor
import kubriko.app.generated.resources.open_scene_editor
import org.jetbrains.compose.resources.stringResource

internal val isSceneEditorVisible = MutableStateFlow(false)
internal actual val sceneJson: MutableStateFlow<String>? = MutableStateFlow("")

@Composable
internal actual fun BoxScope.PlatformSpecificContent() {
    val isEditorVisible = isSceneEditorVisible.collectAsState()
    Button(
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        onClick = { isSceneEditorVisible.value = !isEditorVisible.value }
    ) {
        Text(
            text = stringResource(if (isEditorVisible.value) Res.string.close_scene_editor else Res.string.open_scene_editor)
        )
    }
}