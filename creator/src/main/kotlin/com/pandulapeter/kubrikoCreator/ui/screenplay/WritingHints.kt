/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStyle
import com.pandulapeter.kubrikoCreator.ui.texts.toResource
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.page_writing_hint
import kubriko.creator.generated.resources.page_writing_hints
import org.jetbrains.compose.resources.stringResource

/**
 * Where a page breaks the screenplay's writing rules ([ScreenplayStyle]), shown quietly under it: the design is the user's,
 * so these are hints, never refusals.
 */
@Composable
internal fun WritingHints(
    problems: List<ScreenplayStyle.Problem>,
    modifier: Modifier = Modifier,
) {
    if (problems.isEmpty()) return
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(Res.string.page_writing_hints),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        problems.forEach { problem ->
            Text(
                text = stringResource(Res.string.page_writing_hint, problem.line, problem.excerpt, stringResource(problem.rule.toResource())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
