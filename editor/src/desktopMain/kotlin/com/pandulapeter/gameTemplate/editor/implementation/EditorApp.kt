package com.pandulapeter.gameTemplate.editor.implementation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun EditorApp(
    modifier: Modifier = Modifier,
    openFilePickerForLoading: () -> Unit,
    openFilePickerForSaving: () -> Unit,
) = EditorUserInterface(
    modifier = modifier,
    openFilePickerForLoading = openFilePickerForLoading,
    openFilePickerForSaving = openFilePickerForSaving,
)