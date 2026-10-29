/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.event_blocks_empty
import kubriko.creator.generated.resources.event_none_selected
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Blocks of the selected Event, in the order they run, under a header that also chooses the Event's key or other
 * Actor type.
 *
 * @param listIdentity What the Blocks belong to (the type and the Event), so that a change of it starts over instead of
 *   animating from the Blocks shown before.
 * @param scrollToEndRequest Changes whenever a Block was added at the end (by a click in the palette), which the list
 *   scrolls to so that it is seen.
 */
@Composable
internal fun EventBlocks(
    event: EventDefinition?,
    scope: BlockScope,
    onKeyChange: (KeyChoice) -> Unit,
    onTimerChange: (String) -> Unit,
    onTimerRename: (oldName: String, newName: String) -> Unit,
    onOtherActorTypeChange: (String) -> Unit,
    dragState: BlockDragState,
    onBlockChange: (blockIndex: Int, BlockDefinition) -> Unit,
    onBlockRemoved: (blockIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    highlight: ActorTypeHighlight? = null,
    listIdentity: Any? = null,
    scrollToEndRequest: Int = 0,
) = Column(
    modifier = modifier,
) {
    if (event == null) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(Res.string.event_none_selected),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return@Column
    }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(start = 12.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(event.type.icon),
            contentDescription = null,
            tint = Accent.EVENT.color,
        )
        Text(
            text = stringResource(event.type.title),
            style = MaterialTheme.typography.titleSmall,
        )
        when (event.type) {
            EventType.KEY_PRESSED -> MenuButton(
                label = event.key?.let { stringResource(it.title) }.orEmpty(),
                options = KeyChoice.entries,
                optionText = { stringResource(it.title) },
                onSelected = onKeyChange,
            )
            EventType.TIMER_FINISHED -> (event.timer ?: TimerNaming.DEFAULT_NAME).let { timer ->
                TimerChoice(
                    timer = timer,
                    timerNames = scope.timerNames,
                    onSelected = onTimerChange,
                    onRename = { onTimerRename(timer, it) },
                )
            }
            EventType.COLLISION -> ActorTypeChoice(
                actorType = event.otherActorType.orEmpty(),
                actorTypeNames = scope.actorTypeNames,
                onSelected = onOtherActorTypeChange,
            )
            else -> Unit
        }
    }
    HorizontalDivider()
    BlockList(
        blocks = event.blocks,
        listIdentity = listIdentity,
        scope = scope,
        dragState = dragState,
        onBlockChange = onBlockChange,
        onBlockRemoved = onBlockRemoved,
        highlight = highlight,
        scrollToEndRequest = scrollToEndRequest,
    )
}

/**
 * The Blocks themselves, composed only while they are near the screen, as drop targets for [dragState]. Moves animate,
 * and new Blocks flash, followed by [ListItemKeys]; another [listIdentity] (Event) gets a list of its own, so that
 * switching Events doesn't animate from one's Blocks to the other's.
 */
@Composable
private fun BlockList(
    blocks: List<BlockDefinition>,
    listIdentity: Any?,
    scope: BlockScope,
    dragState: BlockDragState,
    onBlockChange: (blockIndex: Int, BlockDefinition) -> Unit,
    onBlockRemoved: (blockIndex: Int) -> Unit,
    highlight: ActorTypeHighlight?,
    scrollToEndRequest: Int,
) {
    key(listIdentity) {
        val listState = rememberLazyListState()
        val requestShownFirst = remember { scrollToEndRequest }
        LaunchedEffect(scrollToEndRequest) {
            if (scrollToEndRequest != requestShownFirst) listState.animateScrollToItem(blocks.size)
        }
        val itemKeys = remember { ListItemKeys<BlockDefinition>() }
        val keys = remember(itemKeys, blocks) { itemKeys.update(blocks) }
        val draggedKey = rememberDraggedKey((dragState.dragged as? DraggedBlock.Existing)?.index, keys)
        val gap = dragState.gap
        val nesting = remember(blocks) { BlockNesting.of(blocks).first }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .lazyListDropTarget(dragState, listState, blocks.size, BLOCK_CONTENT_TYPE)
                .padding(start = 12.dp, end = 8.dp),
            state = listState,
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (blocks.isEmpty()) {
                item(key = EMPTY_KEY) {
                    Text(
                        modifier = Modifier.padding(4.dp),
                        text = stringResource(Res.string.event_blocks_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(count = blocks.size, key = { keys[it] }, contentType = { BLOCK_CONTENT_TYPE }) { index ->
                Column(
                    modifier = Modifier.animateItem().zIndex(if (keys[index] == draggedKey) 1f else 0f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DropIndicator(isVisible = gap == index)
                    BlockRow(
                        block = blocks[index],
                        nesting = nesting[index],
                        scope = scope,
                        index = index,
                        dragState = dragState,
                        onBlockChange = { onBlockChange(index, it) },
                        onRemove = { onBlockRemoved(index) },
                        flashKey = highlight?.key?.takeIf { highlight.blockIndex == index } ?: keys[index].takeIf(itemKeys::isRecentlyAdded),
                    )
                }
            }
            item(key = END_KEY) {
                DropIndicator(isVisible = gap == blocks.size)
            }
        }
    }
}

private const val EMPTY_KEY = "empty"
private const val END_KEY = "end"
private const val BLOCK_CONTENT_TYPE = "block"

/**
 * Where a dragged Block would land: a line between two Blocks.
 */
@Composable
private fun DropIndicator(isVisible: Boolean) = Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(3.dp)
        .background(if (isVisible) MaterialTheme.colorScheme.primary else Color.Transparent, MaterialTheme.shapes.small),
)
