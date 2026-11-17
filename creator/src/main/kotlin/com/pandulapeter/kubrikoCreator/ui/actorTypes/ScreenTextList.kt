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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenPosition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.BufferedTextField
import com.pandulapeter.kubrikoCreator.ui.components.ColorField
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.NumberField
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.SearchableChoiceButton
import com.pandulapeter.kubrikoCreator.ui.components.hoverReveal
import com.pandulapeter.kubrikoCreator.ui.components.rememberHoverRevealState
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import com.pandulapeter.kubrikoCreator.ui.components.revealedWhen
import com.pandulapeter.kubrikoCreator.ui.components.rightClickMenu
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_text
import kubriko.creator.generated.resources.action_remove
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_text
import kubriko.creator.generated.resources.manager_type_text
import kubriko.creator.generated.resources.manager_type_text_fonts_empty
import kubriko.creator.generated.resources.manager_type_text_shown_always
import kubriko.creator.generated.resources.manager_type_text_shown_while
import kubriko.creator.generated.resources.manager_type_text_size
import kubriko.creator.generated.resources.manager_type_texts
import kubriko.creator.generated.resources.manager_type_texts_empty
import org.jetbrains.compose.resources.stringResource

/**
 * The texts a Manager type shows on top of the game, each with its words (which may show Properties), place, size,
 * font (a family, or one of the game's [fonts], by their paths from `composeResources`), and color.
 */
@Composable
internal fun ScreenTextList(
    texts: List<ScreenTextDefinition>,
    scope: BlockScope,
    fonts: List<String>,
    onAdded: () -> Unit,
    onChanged: (index: Int, ScreenTextDefinition) -> Unit,
    onRemoved: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_text,
        accent = Accent.CREW,
        title = stringResource(Res.string.manager_type_texts),
    ) {
        IconActionButton(
            icon = Res.drawable.ic_add,
            label = Res.string.action_add_text,
            onClick = onAdded,
        )
    }
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (texts.isEmpty()) {
            Text(
                text = stringResource(Res.string.manager_type_texts_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val itemKeys = remember { ListItemKeys<ScreenTextDefinition>() }
        val keys = remember(itemKeys, texts) { itemKeys.update(texts) }
        texts.forEachIndexed { index, text ->
            key(keys[index]) {
                ScreenTextRow(
                    text = text,
                    scope = scope,
                    fonts = fonts,
                    onChange = { onChanged(index, it) },
                    onRemove = { onRemoved(index) },
                )
            }
        }
    }
}

/**
 * Whether the text is always shown or only while a condition holds, with that condition's parts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScreenTextVisibility(
    condition: ConditionDefinition?,
    scope: BlockScope,
    onConditionChange: (ConditionDefinition?) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    MenuButton(
        label = stringResource(if (condition == null) Res.string.manager_type_text_shown_always else Res.string.manager_type_text_shown_while),
        options = listOf(false, true),
        optionText = { stringResource(if (it) Res.string.manager_type_text_shown_while else Res.string.manager_type_text_shown_always) },
        onSelected = { isConditional ->
            if (isConditional != (condition != null)) onConditionChange(if (isConditional) ScreenTextDefinition.defaultConditionFor(scope.properties) else null)
        },
    )
    if (condition != null) {
        ConditionParameters(
            condition = condition,
            scope = scope,
            onConditionChange = onConditionChange,
            isForText = true,
        )
    }
}

/**
 * The font [text] is drawn in, chosen among the families and the game's [fonts]; a font file the game no longer has
 * shows as missing.
 */
@Composable
private fun TextFontChoice(
    text: ScreenTextDefinition,
    fonts: List<String>,
    onChange: (ScreenTextDefinition) -> Unit,
) {
    val isMissing = text.fontFile?.let { it !in fonts } == true
    val choice = FontChoice.of(text)
    SearchableChoiceButton(
        label = if (isMissing) missingChoiceLabel(choice.title()) else choice.title(),
        options = FontChoice.choicesOf(fonts),
        optionText = { it.title() },
        onSelected = { onChange(text.withFont(it)) },
        isMissing = isMissing,
        hint = stringResource(Res.string.manager_type_text_fonts_empty).takeIf { fonts.isEmpty() },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScreenTextRow(
    text: ScreenTextDefinition,
    scope: BlockScope,
    fonts: List<String>,
    onChange: (ScreenTextDefinition) -> Unit,
    onRemove: () -> Unit,
) {
    val hover = rememberHoverRevealState()
    val menu = rememberRightClickMenuState()
    Column(
        modifier = Modifier.hoverReveal(hover).rightClickMenu(menu),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BufferedTextField(
                modifier = Modifier.weight(1f),
                value = text.text,
                onValueChange = { onChange(text.copy(text = it)) },
                label = stringResource(Res.string.manager_type_text),
            )
            Box {
                IconActionButton(modifier = Modifier.revealedWhen(hover.isActive), icon = Res.drawable.ic_close, label = Res.string.action_remove, onClick = onRemove)
                RightClickMenu(menu, listOf(ContextAction(stringResource(Res.string.action_remove), Res.drawable.ic_close, isDestructive = true, onClick = onRemove)))
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            MenuButton(
                label = stringResource(text.position.title),
                options = ScreenPosition.entries,
                optionText = { stringResource(it.title) },
                onSelected = { onChange(text.copy(position = it)) },
            )
            NumberField(
                textWidth = 40.dp,
                label = Res.string.manager_type_text_size,
                value = text.size,
                onValueChange = { if (it > 0f) onChange(text.copy(size = it)) },
            )
            TextFontChoice(text = text, fonts = fonts, onChange = onChange)
            MenuButton(
                label = stringResource(TextStyleChoice.of(text).title),
                options = TextStyleChoice.entries,
                optionText = { stringResource(it.title) },
                onSelected = { onChange(text.copy(isBold = it.isBold, isItalic = it.isItalic)) },
            )
        }
        ColorField(
            modifier = Modifier.fillMaxWidth(),
            color = text.color,
            onColorChange = { onChange(text.copy(color = it)) },
        )
        ScreenTextVisibility(
            condition = text.shownWhen,
            scope = scope,
            onConditionChange = { onChange(text.copy(shownWhen = it)) },
        )
    }
}
