package com.pandulapeter.kubrikoShowcase

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseContent
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseTheme

private val selectedShowcaseEntry = mutableStateOf<ShowcaseEntry?>(null)

@Composable
fun KubrikoShowcase() = ShowcaseTheme {
    BoxWithConstraints {
        ShowcaseContent(
            shouldUseCompactUi = maxWidth <= 600.dp,
            allShowcaseEntries = ShowcaseEntry.entries,
            getSelectedShowcaseEntry = { selectedShowcaseEntry.value },
            selectedShowcaseEntry = selectedShowcaseEntry.value,
            onShowcaseEntrySelected = { selectedShowcaseEntry.value = it },
        )
    }
}