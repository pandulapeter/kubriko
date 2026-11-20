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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.scenes.ActorStateEdits
import com.pandulapeter.kubrikoCreator.ui.components.BufferedTextField
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import com.pandulapeter.kubrikoCreator.ui.components.NumberField
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.scene_inspector_other_values
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * The values an Actor of a type written by hand keeps in [state] besides its body, each named as its class names it and
 * changed with a field of its kind: a switch for a toggle, a number field for a number, a text field for a text, and the
 * JSON itself for anything else (applied only while it reads as JSON).
 */
@Composable
internal fun HandWrittenStateFields(
    state: String,
    onValueChange: (key: String, value: JsonElement) -> Unit,
) {
    val values = remember(state) { ActorStateEdits.otherValuesOf(state) }
    if (values.isEmpty()) return
    Text(
        text = stringResource(Res.string.scene_inspector_other_values),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    values.forEach { (name, value) ->
        key(name) {
            val shownAs = remember { StateValueKind.of(value) }
            when (val kind = StateValueKind.of(value, shownAs)) {
                is StateValueKind.Toggle -> LabeledSwitch(modifier = Modifier.fillMaxWidth(), label = name, checked = kind.value, onCheckedChange = { onValueChange(name, JsonPrimitive(it)) })
                is StateValueKind.Number -> NumberField(
                    modifier = Modifier.fillMaxWidth(),
                    label = name,
                    value = kind.value,
                    onValueChange = { onValueChange(name, kind.encode(it)) },
                )
                is StateValueKind.Text -> BufferedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = name,
                    value = kind.value,
                    onValueChange = { onValueChange(name, JsonPrimitive(it)) },
                )
                is StateValueKind.Raw -> RawValueField(name = name, json = kind.json) { onValueChange(name, it) }
            }
        }
    }
}

/**
 * The JSON of a value, which keeps the text as typed while it means what the state holds, since the state writes it back
 * without its spaces and line breaks.
 */
@Composable
private fun RawValueField(name: String, json: String, onValueChange: (JsonElement) -> Unit) {
    var shown by remember { mutableStateOf(json) }
    var isValid by remember { mutableStateOf(true) }
    LaunchedEffect(json) {
        if (StateValueKind.parse(shown) != StateValueKind.parse(json)) {
            shown = json
            isValid = true
        }
    }
    BufferedTextField(
        modifier = Modifier.fillMaxWidth(),
        label = name,
        value = shown,
        isError = !isValid,
        isSingleLine = false,
        onValueChange = { text ->
            val parsed = StateValueKind.parse(text)
            isValid = parsed != null
            if (parsed != null) {
                shown = text
                onValueChange(parsed)
            }
        },
    )
}

/**
 * How a value of a saved state is edited.
 */
internal sealed interface StateValueKind {
    data class Toggle(val value: Boolean) : StateValueKind

    /**
     * A number a float holds exactly enough; one saved without a fraction is written back without one, rounded, since a
     * whole-number property can't read a fraction.
     */
    data class Number(val value: Float, val isWhole: Boolean) : StateValueKind {
        fun encode(value: Float): JsonPrimitive = if (isWhole) JsonPrimitive(value.roundToLong()) else JsonPrimitive(value)
    }

    data class Text(val value: String) : StateValueKind

    /**
     * Anything else, edited as JSON: a list, an object, a null, or a whole number too large for a float to hold.
     */
    data class Raw(val json: String) : StateValueKind

    companion object {
        /**
         * How [value] is edited, as JSON still if it was edited as JSON when its field was first shown ([shownAs]), so
         * typing a number into a field holding a null doesn't turn the field into another under the caret.
         */
        fun of(value: JsonElement, shownAs: StateValueKind): StateValueKind = if (shownAs is Raw) Raw(value.toString()) else of(value)

        fun of(value: JsonElement): StateValueKind {
            val primitive = value as? JsonPrimitive ?: return Raw(value.toString())
            if (primitive.isString) return Text(primitive.content)
            primitive.booleanOrNull?.let { return Toggle(it) }
            val number = primitive.floatOrNull ?: return Raw(primitive.toString())
            val isWhole = primitive.content.none { it == '.' || it == 'e' || it == 'E' }
            return if (isWhole && abs(number) > LARGEST_EXACT_WHOLE_FLOAT) Raw(primitive.content) else Number(number, isWhole)
        }

        /**
         * [text] read as JSON, or null if it isn't.
         */
        fun parse(text: String): JsonElement? = try {
            Json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

        private const val LARGEST_EXACT_WHOLE_FLOAT = 16_777_216f
    }
}
