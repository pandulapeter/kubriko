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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.implementation.isRunningOnAndroid
import com.pandulapeter.kubriko.implementation.isRunningOnIpad
import com.pandulapeter.kubriko.implementation.isRunningOnIphone
import kotlinx.browser.window
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.ic_disclaimer
import kubriko.app.generated.resources.welcome_disclaimer
import kubriko.app.generated.resources.welcome_disclaimer_web_android
import kubriko.app.generated.resources.welcome_disclaimer_web_general
import kubriko.app.generated.resources.welcome_disclaimer_web_ipad
import kubriko.app.generated.resources.welcome_disclaimer_web_iphone
import kubriko.app.generated.resources.welcome_disclaimer_web_not_chrome_or_firefox
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun Disclaimer() = Column(
    modifier = Modifier
        .padding(16.dp)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(Res.drawable.ic_disclaimer),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = stringResource(Res.string.welcome_disclaimer),
        )
        Text(
            color = MaterialTheme.colorScheme.primary,
            text = stringResource(Res.string.welcome_disclaimer),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
    Text(
        text = stringResource(Res.string.welcome_disclaimer_web_general),
        style = MaterialTheme.typography.bodySmall,
    )
    if (window.isRunningOnIphone()) {
        Text(
            text = stringResource(Res.string.welcome_disclaimer_web_iphone),
            style = MaterialTheme.typography.bodySmall,
        )
    } else if (window.isRunningOnIpad()) {
        Text(
            text = stringResource(Res.string.welcome_disclaimer_web_ipad),
            style = MaterialTheme.typography.bodySmall,
        )
    } else if (window.isRunningOnAndroid()) {
        Text(
            text = stringResource(Res.string.welcome_disclaimer_web_android),
            style = MaterialTheme.typography.bodySmall,
        )
    } else if (window.navigator.userAgent.let { !it.contains("Chrome") && !it.contains("Firefox") }) {
        Text(
            text = stringResource(Res.string.welcome_disclaimer_web_not_chrome_or_firefox),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}