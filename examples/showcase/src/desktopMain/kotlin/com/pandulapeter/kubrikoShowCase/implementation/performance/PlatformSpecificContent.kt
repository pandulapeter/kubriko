package com.pandulapeter.kubrikoShowcase.implementation.performance

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kubriko.examples.showcase.generated.resources.Res
import kubriko.examples.showcase.generated.resources.editor
import kubriko.examples.showcase.generated.resources.reload
import org.jetbrains.compose.resources.stringResource

internal val isSceneEditorVisible = MutableStateFlow(false)

@Composable
internal actual fun BoxScope.PlatformSpecificContent(
    onReloadClicked: () -> Unit,
) = Column(
    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
    horizontalAlignment = Alignment.End,
) {
    Button(
        onClick = { isSceneEditorVisible.value = !isSceneEditorVisible.value }
    ) {
        Text(
            text = stringResource(Res.string.editor)
        )
    }
    Button(
        onClick = onReloadClicked,
    ) {
        Text(
            text = stringResource(Res.string.reload)
        )
    }
}