package com.pandulapeter.kubrikoShowcase

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ui.ShowcaseContent
import com.pandulapeter.kubrikoShowcase.implementation.ui.getStateHolder

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
            onShowcaseEntrySelected = { showcaseEntry ->
                selectedShowcaseEntry.value?.getStateHolder()?.stopMusic()
                selectedShowcaseEntry.value = showcaseEntry
            },
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )
    }
}

private val selectedShowcaseEntry = mutableStateOf<ShowcaseEntry?>(null)