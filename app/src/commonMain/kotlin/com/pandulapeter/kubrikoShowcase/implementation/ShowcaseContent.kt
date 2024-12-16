package com.pandulapeter.kubrikoShowcase.implementation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.demoContentShaders.ContentShadersDemo
import com.pandulapeter.kubriko.demoContentShaders.ContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoContentShaders.createContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoInput.InputDemo
import com.pandulapeter.kubriko.demoInput.InputDemoStateHolder
import com.pandulapeter.kubriko.demoInput.createInputDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemo
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.createPerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemo
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.createPhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.ShaderAnimationsDemo
import com.pandulapeter.kubriko.demoShaderAnimations.ShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.demoShaderAnimations.createShaderAnimationsDemoStateHolder
import com.pandulapeter.kubriko.gameSpaceSquadron.SpaceSquadronGame
import com.pandulapeter.kubriko.gameSpaceSquadron.SpaceSquadronGameStateHolder
import com.pandulapeter.kubriko.gameSpaceSquadron.createSpaceSquadronGameStateHolder
import com.pandulapeter.kubriko.gameWallbreaker.WallbreakerGame
import com.pandulapeter.kubriko.gameWallbreaker.WallbreakerGameStateHolder
import com.pandulapeter.kubriko.gameWallbreaker.createWallbreakerGameStateHolder
import com.pandulapeter.kubriko.shared.ExampleStateHolder
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

private val currentDemoStateHolders = mutableStateOf(emptyList<ExampleStateHolder>())

@Composable
internal fun ShowcaseContent(
    shouldUseCompactUi: Boolean,
    allShowcaseEntries: List<ShowcaseEntry>,
    getSelectedShowcaseEntry: () -> ShowcaseEntry?,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
) = Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
        HeaderWrapper(
            shouldUseCompactUi = shouldUseCompactUi,
            selectedShowcaseEntry = selectedShowcaseEntry,
            onShowcaseEntrySelected = onShowcaseEntrySelected,
        )
    }
) { paddingValues ->
    val lazyListState = rememberLazyListState()
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues)
    ) {
        @Composable
        fun ShowcaseEntry.content() {
            val modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Right))
            when (this) {

                ShowcaseEntry.WALLBREAKER -> WallbreakerGame(
                    modifier = modifier,
                    stateHolder = getOrCreateState(currentDemoStateHolders, ::createWallbreakerGameStateHolder),
                )

                ShowcaseEntry.SPACE_SQUADRON -> SpaceSquadronGame(
                    modifier = modifier,
                    stateHolder = getOrCreateState(currentDemoStateHolders, ::createSpaceSquadronGameStateHolder),
                )

                ShowcaseEntry.CONTENT_SHADERS -> ContentShadersDemo(
                    modifier = modifier,
                    stateHolder = getOrCreateState(currentDemoStateHolders, ::createContentShadersDemoStateHolder),
                )

                ShowcaseEntry.INPUT -> InputDemo(
                    modifier = modifier,
                    stateHolder = getOrCreateState(currentDemoStateHolders, ::createInputDemoStateHolder),
                )

                ShowcaseEntry.PERFORMANCE -> PerformanceDemo(
                    modifier = modifier,
                    stateHolder = getOrCreateState(currentDemoStateHolders, ::createPerformanceDemoStateHolder),
                )

                ShowcaseEntry.PHYSICS -> PhysicsDemo(
                    modifier = modifier,
                    stateHolder = getOrCreateState(currentDemoStateHolders, ::createPhysicsDemoStateHolder),
                )

                ShowcaseEntry.SHADER_ANIMATIONS -> ShaderAnimationsDemo(
                    modifier = modifier,
                    stateHolder = getOrCreateState(currentDemoStateHolders, ::createShaderAnimationsDemoStateHolder),
                )
            }
            DisposableEffect(type) {
                onDispose {
                    if (getSelectedShowcaseEntry() != selectedShowcaseEntry) {
                        stateHolderType.let { type ->
                            currentDemoStateHolders.value.filter { type.isInstance(it) }.forEach { it.dispose() }
                            currentDemoStateHolders.value = currentDemoStateHolders.value.filterNot { type.isInstance(it) }
                        }
                    }
                }
            }
        }

        if (shouldUseCompactUi) {
            AnimatedVisibility(
                visible = selectedShowcaseEntry == null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
                    state = lazyListState,
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
                    showcaseEntry?.content()
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
            ) {
                Spacer(
                    modifier = Modifier.width(
                        200.dp + WindowInsets.safeDrawing.only(WindowInsetsSides.Left).asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
                    )
                )
                Crossfade(
                    modifier = Modifier.weight(1f),
                    targetState = selectedShowcaseEntry,
                ) { showcaseEntry ->
                    showcaseEntry?.content() ?: WelcomeMessage()
                }
            }
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    modifier = Modifier.width(
                        200.dp + WindowInsets.safeDrawing.only(WindowInsetsSides.Left).asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
                    ).fillMaxHeight(),
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
                        contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
                        state = lazyListState,
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

private inline fun <reified T : ExampleStateHolder> getOrCreateState(
    stateHolders: MutableState<List<ExampleStateHolder>>,
    creator: () -> T
): T = stateHolders.value.filterIsInstance<T>().firstOrNull() ?: creator().also { stateHolders.value += it }

private val ShowcaseEntry.stateHolderType
    get() = when (this) {
        ShowcaseEntry.CONTENT_SHADERS -> ContentShadersDemoStateHolder::class
        ShowcaseEntry.SHADER_ANIMATIONS -> ShaderAnimationsDemoStateHolder::class
        ShowcaseEntry.INPUT -> InputDemoStateHolder::class
        ShowcaseEntry.PERFORMANCE -> PerformanceDemoStateHolder::class
        ShowcaseEntry.PHYSICS -> PhysicsDemoStateHolder::class
        ShowcaseEntry.SPACE_SQUADRON -> SpaceSquadronGameStateHolder::class
        ShowcaseEntry.WALLBREAKER -> WallbreakerGameStateHolder::class
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
    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
        .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Left).asPaddingValues())
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
        )
        .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Left).asPaddingValues()),
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold,
    style = MaterialTheme.typography.labelSmall,
    text = stringResource(title),
)