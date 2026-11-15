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

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.MusicValue
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.audio_music_none_yet
import kubriko.creator.generated.resources.block_music_choose
import kubriko.creator.generated.resources.ic_music
import kubriko.creator.generated.resources.music_value_fixed
import kubriko.creator.generated.resources.music_value_kind
import kubriko.creator.generated.resources.music_value_property

/**
 * The music a Block plays: a file of the project, or the one a music Property holds, chosen beside it.
 */
@Composable
internal fun MusicValueField(
    value: MusicValue,
    scope: BlockScope,
    onValueChange: (MusicValue) -> Unit,
) = FixedOrPropertyField(
    property = value.propertyReference,
    scope = scope,
    fixedIcon = Res.drawable.ic_music,
    kindLabel = Res.string.music_value_kind,
    fixedLabel = Res.string.music_value_fixed,
    propertyLabel = Res.string.music_value_property,
    onKindChange = { toProperty -> onValueChange(if (toProperty) MusicValue.Property() else MusicValue.Fixed("")) },
    onPropertyChange = { onValueChange(MusicValue.Property(property = it.name, manager = it.manager)) },
) {
    AudioFileChoice((value as MusicValue.Fixed).path, scope.audioFiles?.music, Res.string.block_music_choose, Res.string.audio_music_none_yet) { onValueChange(MusicValue.Fixed(it)) }
}
