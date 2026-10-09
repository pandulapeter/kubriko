/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui.licenses

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kubriko.app.shared.generated.resources.Res
import kubriko.app.shared.generated.resources.other_licenses_content
import kubriko.app.shared.generated.resources.other_licenses_music_note
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LicensesScreen(
    modifier: Modifier = Modifier,
    stateHolder: LicensesScreenStateHolder = createLicensesScreenStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    scrollState: ScrollState = rememberScrollState(),
) = Column(
    modifier = modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Right))
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(
        modifier = Modifier.padding(bottom = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        text = stringResource(Res.string.other_licenses_content),
    )
    Dependency.entries.groupBy { it.type }.forEach { (type, dependencies) ->
        Text(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            text = stringResource(type.licenseName),
        )
        val uriHandler = LocalUriHandler.current
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            dependencies.forEach { dependency ->
                Text(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = LocalContentColor.current.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { uriHandler.openUri(dependency.url) }
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    text = dependency.dependencyName,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    Text(
        style = MaterialTheme.typography.bodySmall,
        text = stringResource(Res.string.other_licenses_music_note),
    )
}