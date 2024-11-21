package com.pandulapeter.kubrikoShowcase.implementation.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.open_scene_editor
import kubriko.app.generated.resources.editor_disclaimer
import org.jetbrains.compose.resources.stringResource

internal val isSceneEditorVisible = MutableStateFlow(false)

@Composable
internal actual fun BoxScope.PlatformSpecificContent() = Column(
    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Card {
        Text(
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
            text = stringResource(Res.string.editor_disclaimer)
        )
    }
    Button(
        onClick = { isSceneEditorVisible.value = !isSceneEditorVisible.value }
    ) {
        Text(
            text = stringResource(Res.string.open_scene_editor)
        )
    }
}