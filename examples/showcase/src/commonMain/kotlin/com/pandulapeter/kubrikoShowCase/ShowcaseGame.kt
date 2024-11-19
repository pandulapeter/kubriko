package com.pandulapeter.kubrikoShowcase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import kubriko.examples.showcase.generated.resources.Res
import kubriko.examples.showcase.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource

@Composable
fun ShowcaseGame(
    modifier: Modifier = Modifier,
) = MaterialTheme {
    val selectedShowcaseEntry = rememberSaveable { mutableStateOf<ShowcaseEntry?>(null) }
    BoxWithConstraints(
        modifier = modifier,
    ) {
        Content(
            shouldUseCompactUi = maxWidth <= 600.dp,
            allShowcaseEntries = ShowcaseEntry.entries,
            selectedShowcaseEntry = selectedShowcaseEntry.value,
            onShowcaseEntrySelected = { selectedShowcaseEntry.value = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    modifier: Modifier = Modifier,
    shouldUseCompactUi: Boolean,
    allShowcaseEntries: List<ShowcaseEntry>,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
) = Scaffold(
    modifier = modifier,
    topBar = {
        TopAppBar(
            title = { Text(text = if (shouldUseCompactUi && selectedShowcaseEntry != null) selectedShowcaseEntry.title else "Kubriko Showcase") },
            navigationIcon = {
                if (shouldUseCompactUi && selectedShowcaseEntry != null) {
                    Icon(
                        modifier = modifier
                            .clip(CircleShape)
                            .clickable { onShowcaseEntrySelected(null) }
                            .padding(4.dp),
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = "Back",
                    )
                }
            }
        )
    }
) { paddingValues ->
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues)
    ) {
        if (shouldUseCompactUi) {
            if (selectedShowcaseEntry == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item("welcome") {
                        WelcomeMessage()
                    }
                    menu(
                        allShowcaseEntries = allShowcaseEntries,
                        selectedShowcaseEntry = selectedShowcaseEntry,
                        onShowcaseEntrySelected = onShowcaseEntrySelected,
                    )
                }
            } else {
                selectedShowcaseEntry.content.invoke()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.width(200.dp).fillMaxHeight(),
                ) {
                    menu(
                        allShowcaseEntries = allShowcaseEntries,
                        selectedShowcaseEntry = selectedShowcaseEntry,
                        onShowcaseEntrySelected = onShowcaseEntrySelected,
                    )
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    selectedShowcaseEntry?.content?.invoke() ?: WelcomeMessage()
                }
            }
        }
    }
}

@Composable
private fun WelcomeMessage(
    modifier: Modifier = Modifier,
) = Text(
    modifier = modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
    style = MaterialTheme.typography.bodySmall,
    text = "Welcome!\n\nThis is the Showcase app for the Kubriko game engine.",
)

private fun LazyListScope.menu(
    allShowcaseEntries: List<ShowcaseEntry>,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
) = items(
    items = allShowcaseEntries,
    key = { it.name }
) { showcaseEntry ->
    (selectedShowcaseEntry == showcaseEntry).let { isSelected ->
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = isSelected,
                    onClick = { onShowcaseEntrySelected(if (isSelected) null else showcaseEntry) },
                )
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            text = showcaseEntry.title,
        )
    }
}