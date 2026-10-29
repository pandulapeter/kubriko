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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_sprite
import kubriko.creator.generated.resources.actor_type_sprite_none
import kubriko.creator.generated.resources.actor_type_sprites_empty
import org.jetbrains.compose.resources.stringResource

/**
 * A choice of the image an Actor type is drawn as, or none, for its shape, with a hint where images go while the
 * project has none.
 *
 * @param sprite The chosen image's path from `composeResources`, or null for none.
 * @param onSpriteChange Called with the chosen path, or an empty text for none.
 */
@Composable
internal fun SpriteChoice(
    sprite: String?,
    sprites: List<String>,
    onSpriteChange: (String) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.actor_type_sprite),
            style = MaterialTheme.typography.labelMedium,
        )
        MenuButton(
            label = sprite?.let(::spriteLabel) ?: stringResource(Res.string.actor_type_sprite_none),
            options = listOf("") + sprites,
            optionText = { it.ifEmpty { null }?.let(::spriteLabel) ?: stringResource(Res.string.actor_type_sprite_none) },
            onSelected = onSpriteChange,
        )
    }
    if (sprites.isEmpty()) {
        Text(
            text = stringResource(Res.string.actor_type_sprites_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun spriteLabel(path: String) = path.removePrefix("drawable/")
