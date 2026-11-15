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
import com.pandulapeter.kubrikoCreator.actors.SoundValue
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.audio_sounds_none_yet
import kubriko.creator.generated.resources.block_sound_choose
import kubriko.creator.generated.resources.ic_sound
import kubriko.creator.generated.resources.sound_value_fixed
import kubriko.creator.generated.resources.sound_value_kind
import kubriko.creator.generated.resources.sound_value_property

/**
 * The sound a Block plays: a file of the project, or the one a sound Property holds, chosen beside it.
 */
@Composable
internal fun SoundValueField(
    value: SoundValue,
    scope: BlockScope,
    onValueChange: (SoundValue) -> Unit,
) = FixedOrPropertyField(
    property = value.propertyReference,
    scope = scope,
    fixedIcon = Res.drawable.ic_sound,
    kindLabel = Res.string.sound_value_kind,
    fixedLabel = Res.string.sound_value_fixed,
    propertyLabel = Res.string.sound_value_property,
    onKindChange = { toProperty -> onValueChange(if (toProperty) SoundValue.Property() else SoundValue.Fixed("")) },
    onPropertyChange = { onValueChange(SoundValue.Property(property = it.name, manager = it.manager)) },
) {
    AudioFileChoice((value as SoundValue.Fixed).path, scope.audioFiles?.sounds, Res.string.block_sound_choose, Res.string.audio_sounds_none_yet) { onValueChange(SoundValue.Fixed(it)) }
}
