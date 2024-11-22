package com.pandulapeter.kubrikoShowcase

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseTheme
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.back
import kubriko.app.generated.resources.ic_back
import kubriko.app.generated.resources.kubriko_showcase
import kubriko.app.generated.resources.welcome
import kubriko.app.generated.resources.welcome_message
import kubriko.app.generated.resources.welcome_subtitle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun KubrikoShowcase(
    modifier: Modifier = Modifier,
) = ShowcaseTheme {
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
        HeaderWrapper(
            shouldUseCompactUi = shouldUseCompactUi,
            selectedShowcaseEntry = selectedShowcaseEntry,
            onShowcaseEntrySelected = onShowcaseEntrySelected,
        )
    }
) { paddingValues ->
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues)
    ) {
        if (shouldUseCompactUi) {
            AnimatedVisibility(
                visible = selectedShowcaseEntry == null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
            }
            AnimatedVisibility(
                visible = selectedShowcaseEntry != null,
                enter = slideIn { IntOffset(0, it.height / 10) },
                exit = slideOut { IntOffset(0, it.height / 10) },
            ) {
                Crossfade(
                    targetState = selectedShowcaseEntry,
                ) { showcaseEntry ->
                    showcaseEntry?.content?.invoke()
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
            ) {
                Spacer(modifier = Modifier.width(200.dp))
                Crossfade(
                    modifier = Modifier.weight(1f),
                    targetState = selectedShowcaseEntry,
                ) { showcaseEntry ->
                    showcaseEntry?.content?.invoke() ?: WelcomeMessage()
                }
            }
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    modifier = Modifier.width(200.dp).fillMaxHeight(),
                    tonalElevation = when (isSystemInDarkTheme()) {
                        true -> 4.dp
                        false -> 0.dp
                    },
                    shadowElevation = when (isSystemInDarkTheme()) {
                        true -> 4.dp
                        false -> 2.dp
                    },
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            MenuItem(
                                isSelected = selectedShowcaseEntry == null,
                                title = Res.string.welcome,
                                subtitle = Res.string.welcome_subtitle,
                                onSelected = { onShowcaseEntrySelected(null) },
                            )
                        }
                        menu(
                            allShowcaseEntries = allShowcaseEntries,
                            selectedShowcaseEntry = selectedShowcaseEntry,
                            onShowcaseEntrySelected = onShowcaseEntrySelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderWrapper(
    modifier: Modifier = Modifier,
    shouldUseCompactUi: Boolean,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
) = Surface(
    modifier = modifier,
    tonalElevation = when (isSystemInDarkTheme()) {
        true -> 4.dp
        false -> 0.dp
    },
    shadowElevation = when (isSystemInDarkTheme()) {
        true -> 4.dp
        false -> 2.dp
    },
) {
    if (shouldUseCompactUi) {
        Crossfade(
            targetState = selectedShowcaseEntry
        ) { showcaseEntry ->
            Header(
                modifier = Modifier.fillMaxWidth(),
                shouldUseCompactUi = shouldUseCompactUi,
                selectedShowcaseEntry = showcaseEntry,
                onShowcaseEntrySelected = onShowcaseEntrySelected,
            )
        }
    } else {
        Header(
            modifier = Modifier.fillMaxWidth(),
            shouldUseCompactUi = shouldUseCompactUi,
            selectedShowcaseEntry = selectedShowcaseEntry,
            onShowcaseEntrySelected = onShowcaseEntrySelected,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header(
    modifier: Modifier = Modifier,
    shouldUseCompactUi: Boolean,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
) = TopAppBar(
    modifier = modifier,
    title = {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(
                    resource = if (shouldUseCompactUi && selectedShowcaseEntry != null) {
                        selectedShowcaseEntry.titleStringResource
                    } else {
                        Res.string.kubriko_showcase
                    }
                ),
            )
            if (shouldUseCompactUi && selectedShowcaseEntry != null) {
                Text(
                    color = LocalContentColor.current.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.titleSmall,
                    text = stringResource(selectedShowcaseEntry.subtitleStringResource),
                )
            }
        }
    },
    navigationIcon = {
        if (shouldUseCompactUi && selectedShowcaseEntry != null) {
            IconButton(
                onClick = { onShowcaseEntrySelected(null) }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_back),
                    contentDescription = stringResource(Res.string.back),
                )
            }
        }
    }
)

@Composable
private fun WelcomeMessage(
    modifier: Modifier = Modifier,
) = Text(
    modifier = modifier.padding(16.dp),
    style = MaterialTheme.typography.bodySmall,
    text = stringResource(Res.string.welcome_message),
)

private fun LazyListScope.menu(
    allShowcaseEntries: List<ShowcaseEntry>,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
) = allShowcaseEntries.groupBy { it.type }.let { groups ->
    groups.forEach { (type, entries) ->
        item(type.name) {
            MenuCategoryLabel(title = type.titleStringResource)
        }
        items(
            items = entries,
            key = { it.name }
        ) { showcaseEntry ->
            MenuItem(
                isSelected = selectedShowcaseEntry == showcaseEntry,
                title = showcaseEntry.titleStringResource,
                subtitle = showcaseEntry.subtitleStringResource,
                onSelected = { onShowcaseEntrySelected(showcaseEntry) },
            )
        }
    }
}

@Composable
private fun MenuItem(
    isSelected: Boolean,
    title: StringResource,
    subtitle: StringResource,
    onSelected: () -> Unit,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .selectable(
            selected = isSelected,
            onClick = onSelected,
        )
        .background(
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        )
        .padding(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelLarge,
        text = stringResource(title),
    )
    Text(
        modifier = Modifier.fillMaxWidth(),
        color = LocalContentColor.current.copy(alpha = 0.75f),
        style = MaterialTheme.typography.labelSmall,
        text = stringResource(subtitle),
    )
}

@Composable
private fun MenuCategoryLabel(
    title: StringResource,
) = Text(
    modifier = Modifier
        .fillMaxWidth()
        .padding(
            horizontal = 16.dp,
            vertical = 4.dp,
        ),
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold,
    style = MaterialTheme.typography.labelSmall,
    text = stringResource(title),
)