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
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.choice_missing
import org.jetbrains.compose.resources.stringResource

/**
 * Whether [value], what a Block or an Event names, is none of [options], when they are known: what is left of a thing
 * deleted outside Kubriko Creator, which its Blocks still name.
 */
internal fun isMissingChoice(value: String, options: List<String>?) = options != null && value.isNotEmpty() && value !in options

/**
 * What a choice shows for [name] once it no longer exists ([isMissingChoice]).
 */
@Composable
internal fun missingChoiceLabel(name: String) = stringResource(Res.string.choice_missing, name)
