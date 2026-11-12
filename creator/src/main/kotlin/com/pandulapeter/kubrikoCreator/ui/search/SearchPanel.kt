/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_search
import kubriko.creator.generated.resources.search_title
import org.jetbrains.compose.resources.stringResource

/**
 * The Search panel: what refers to [usagesSubject] ([UsageResults]), each going to its place when clicked.
 */
@Composable
internal fun SearchPanel(
    usagesSubject: RelationSubject?,
    usages: UsagesState,
    onUsageSelected: (Usage) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_search,
        title = stringResource(Res.string.search_title),
    )
    HorizontalDivider()
    if (usagesSubject != null) {
        UsageResults(
            subject = usagesSubject,
            usages = usages,
            onUsageSelected = onUsageSelected,
        )
    }
}
