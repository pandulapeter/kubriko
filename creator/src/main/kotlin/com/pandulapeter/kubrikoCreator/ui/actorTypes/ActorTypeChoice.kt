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
import com.pandulapeter.kubrikoCreator.ui.components.SearchableChoiceButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_none
import kubriko.creator.generated.resources.event_collision_choose
import org.jetbrains.compose.resources.stringResource

/**
 * Which of the project's Actor types a Block is about, or, where [isNoneOffered] (an Actor type reference Property's
 * value), none: the empty name.
 */
@Composable
internal fun ActorTypeChoice(
    actorType: String,
    actorTypeNames: List<String>,
    isNoneOffered: Boolean = false,
    onSelected: (String) -> Unit,
) = SearchableChoiceButton(
    label = when {
        isMissingChoice(actorType, actorTypeNames) -> missingChoiceLabel(actorType)
        actorType.isEmpty() && isNoneOffered -> stringResource(Res.string.actor_type_none)
        else -> actorType.ifEmpty { stringResource(Res.string.event_collision_choose) }
    },
    options = if (isNoneOffered) listOf("") + actorTypeNames else actorTypeNames,
    optionText = { it.ifEmpty { stringResource(Res.string.actor_type_none) } },
    onSelected = onSelected,
    isMissing = isMissingChoice(actorType, actorTypeNames),
)
