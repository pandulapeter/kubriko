package com.pandulapeter.kubrikoShowcase

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shared.ui.theme.KubrikoTheme
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ui.ShowcaseContent

@Composable
fun KubrikoShowcase(
    isInFullscreenMode: Boolean,
    onFullscreenModeToggled: () -> Unit,
) = KubrikoTheme {
    BoxWithConstraints {
        ShowcaseContent(
            shouldUseCompactUi = maxWidth <= 600.dp,
            allShowcaseEntries = ShowcaseEntry.entries,
            getSelectedShowcaseEntry = { selectedShowcaseEntry.value },
            selectedShowcaseEntry = selectedShowcaseEntry.value,
            onShowcaseEntrySelected = { selectedShowcaseEntry.value = it },
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )
    }
}

private val selectedShowcaseEntry = mutableStateOf<ShowcaseEntry?>(null)