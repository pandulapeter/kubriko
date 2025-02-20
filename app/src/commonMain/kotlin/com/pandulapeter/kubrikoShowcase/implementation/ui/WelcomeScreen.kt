/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.uiComponents.LargeButton
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.ic_collapse
import kubriko.app.generated.resources.ic_discord
import kubriko.app.generated.resources.ic_documentation
import kubriko.app.generated.resources.ic_expand
import kubriko.app.generated.resources.ic_getting_started
import kubriko.app.generated.resources.ic_github
import kubriko.app.generated.resources.ic_youtube
import kubriko.app.generated.resources.welcome_app_details
import kubriko.app.generated.resources.welcome_app_details_call_to_action_collapsed
import kubriko.app.generated.resources.welcome_app_details_call_to_action_expanded
import kubriko.app.generated.resources.welcome_community
import kubriko.app.generated.resources.welcome_documentation
import kubriko.app.generated.resources.welcome_engine_details
import kubriko.app.generated.resources.welcome_getting_started
import kubriko.app.generated.resources.welcome_hide_details
import kubriko.app.generated.resources.welcome_learning_1
import kubriko.app.generated.resources.welcome_learning_2
import kubriko.app.generated.resources.welcome_license
import kubriko.app.generated.resources.welcome_message
import kubriko.app.generated.resources.welcome_more_details
import kubriko.app.generated.resources.welcome_repository
import kubriko.app.generated.resources.welcome_tutorials
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WelcomeScreen(
    modifier: Modifier = Modifier,
    shouldUseCompactUi: Boolean,
) = Column(
    modifier = modifier.padding(
        top = 16.dp,
        bottom = 8.dp,
    ),
) {
    Text(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        text = stringResource(Res.string.welcome_message),
    )
    Column {
        AnimatedVisibility(
            visible = !shouldUseCompactUi || shouldShowMoreInfo.value,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.CenterVertically),
            exit = shrinkVertically(shrinkTowards = Alignment.CenterVertically) + fadeOut(),
        ) {
            val uriHandler = LocalUriHandler.current
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    text = stringResource(Res.string.welcome_engine_details),
                )
                LargeButton(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    icon = Res.drawable.ic_github,
                    title = Res.string.welcome_repository,
                    onButtonPressed = { uriHandler.openUri("https://github.com/pandulapeter/kubriko") },
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    text = stringResource(Res.string.welcome_learning_1),
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LargeButton(
                        icon = Res.drawable.ic_getting_started,
                        title = Res.string.welcome_getting_started,
                        onButtonPressed = { uriHandler.openUri("https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED.md") },
                    )
                    LargeButton(
                        icon = Res.drawable.ic_documentation,
                        title = Res.string.welcome_documentation,
                        onButtonPressed = { uriHandler.openUri("https://github.com/pandulapeter/kubriko/blob/main/documentation/README.md") },
                    )
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    text = stringResource(Res.string.welcome_learning_2),
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LargeButton(
                        icon = Res.drawable.ic_youtube,
                        title = Res.string.welcome_tutorials,
                        onButtonPressed = { uriHandler.openUri("https://github.com/pandulapeter/kubriko/blob/main/documentation/TUTORIAL_VIDEOS.md") },
                    )
                    LargeButton(
                        icon = Res.drawable.ic_discord,
                        title = Res.string.welcome_community,
                        onButtonPressed = { uriHandler.openUri("https://discord.gg/9CqVUGpK") },
                    )
                }
                Text(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    text = stringResource(Res.string.welcome_license),
                )
            }
        }
    }
    AnimatedVisibility(
        modifier = Modifier.padding(top = 8.dp),
        visible = shouldUseCompactUi,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.CenterVertically),
        exit = shrinkVertically(shrinkTowards = Alignment.CenterVertically) + fadeOut(),
    ) {
        AnimatedContent(
            targetState = !shouldShowMoreInfo.value,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "moreInfoButton",
            contentAlignment = Alignment.TopCenter,
        ) { shouldShowMoreInfoState ->
            Row(
                modifier = Modifier
                    .clickable { shouldShowMoreInfo.value = shouldShowMoreInfoState }
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val text = stringResource(if (shouldShowMoreInfoState) Res.string.welcome_more_details else Res.string.welcome_hide_details)
                Text(
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    text = text,
                )
                Image(
                    painter = painterResource(if (shouldShowMoreInfoState) Res.drawable.ic_expand else Res.drawable.ic_collapse),
                    colorFilter = ColorFilter.tint(LocalContentColor.current),
                    contentDescription = text,
                )
            }
        }
    }
    Text(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        text = stringResource(Res.string.welcome_app_details) + stringResource(if (shouldUseCompactUi) Res.string.welcome_app_details_call_to_action_collapsed else Res.string.welcome_app_details_call_to_action_expanded),
    )
}

private val shouldShowMoreInfo = mutableStateOf(false)