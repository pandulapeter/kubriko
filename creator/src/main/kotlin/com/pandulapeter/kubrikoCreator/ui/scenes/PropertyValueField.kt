/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.scenes.ActorValueChanges
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.ui.actorTypes.ActorTypeChoice
import com.pandulapeter.kubrikoCreator.ui.actorTypes.AudioFileChoice
import com.pandulapeter.kubrikoCreator.ui.actorTypes.ReferenceChoices
import com.pandulapeter.kubrikoCreator.ui.components.BufferedTextField
import com.pandulapeter.kubrikoCreator.ui.components.ColorField
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import com.pandulapeter.kubrikoCreator.ui.components.NumberField
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.longOrNull
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.audio_music_none_yet
import kubriko.creator.generated.resources.audio_sounds_none_yet
import kubriko.creator.generated.resources.block_music_choose
import kubriko.creator.generated.resources.block_sound_choose

/**
 * The value of [property] that a Scene keeps ([value]), or its starting value while it keeps none, labeled with [label].
 */
@Composable
internal fun PropertyValueField(
    property: PropertyDefinition,
    value: JsonPrimitive?,
    onChange: (ActorValueChanges.PropertyValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String = property.name,
    choices: ReferenceChoices = ReferenceChoices(),
) = when (property) {
    is PropertyDefinition.Number -> NumberField(
        modifier = modifier.fillMaxWidth(),
        label = label,
        value = value?.floatOrNull ?: property.initialValue,
        onValueChange = { onChange(ActorValueChanges.PropertyValue(property.name, number = it)) },
    )
    is PropertyDefinition.Text -> BufferedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value?.takeIf { it.isString }?.content ?: property.initialValue,
        onValueChange = { onChange(ActorValueChanges.PropertyValue(property.name, text = it)) },
        label = label,
    )
    is PropertyDefinition.Toggle -> LabeledSwitch(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp),
        label = label,
        checked = value?.booleanOrNull ?: property.initialValue,
        onCheckedChange = { onChange(ActorValueChanges.PropertyValue(property.name, toggle = it)) },
        labelStyle = MaterialTheme.typography.bodyMedium,
    )
    is PropertyDefinition.Color -> ColorField(
        modifier = modifier.fillMaxWidth(),
        color = value?.longOrNull?.let(SceneSettingsState::argbOf) ?: property.initialValue,
        onColorChange = { onChange(ActorValueChanges.PropertyValue(property.name, color = it)) },
        label = label,
    )
    is PropertyDefinition.ActorType -> Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        ActorTypeChoice(
            actorType = value?.takeIf { it.isString }?.content ?: property.initialValue,
            actorTypeNames = choices.actorTypeNames,
            onSelected = { onChange(ActorValueChanges.PropertyValue(property.name, actorType = it)) },
            isNoneOffered = true,
        )
    }
    is PropertyDefinition.AudioFile -> AudioPropertyField(
        property = property,
        label = label,
        path = value?.takeIf { it.isString }?.content ?: property.initialValue,
        audioFiles = choices.audioFiles,
        onChange = onChange,
        modifier = modifier,
    )
}

@Composable
private fun AudioPropertyField(
    property: PropertyDefinition.AudioFile,
    label: String,
    path: String,
    audioFiles: AudioFiles?,
    onChange: (ActorValueChanges.PropertyValue) -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val isMusic = property is PropertyDefinition.Music
    Text(text = label, style = MaterialTheme.typography.bodyMedium)
    AudioFileChoice(
        path = path,
        paths = if (isMusic) audioFiles?.music else audioFiles?.sounds,
        placeholder = if (isMusic) Res.string.block_music_choose else Res.string.block_sound_choose,
        noneYet = if (isMusic) Res.string.audio_music_none_yet else Res.string.audio_sounds_none_yet,
        canChooseNone = true,
        onSelected = { onChange(if (isMusic) ActorValueChanges.PropertyValue(property.name, music = it) else ActorValueChanges.PropertyValue(property.name, sound = it)) },
    )
}
