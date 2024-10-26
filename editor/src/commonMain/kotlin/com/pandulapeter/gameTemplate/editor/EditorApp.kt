package com.pandulapeter.gameTemplate.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.gameTemplate.editor.implementation.EditorUserInterface

@Composable
fun EditorApp(
    modifier: Modifier = Modifier,
) = EditorUserInterface(modifier)