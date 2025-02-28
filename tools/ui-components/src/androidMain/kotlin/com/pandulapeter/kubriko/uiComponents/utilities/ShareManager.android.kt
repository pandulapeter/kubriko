/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents.utilities

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class ShareManagerImpl(
    private val context: Context,
) : ShareManager {
    override val isSharingSupported = true

    override fun shareText(text: String) = context.startActivity(
        Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }, null)
    )
}

@Composable
actual fun rememberShareManager(): ShareManager {
    val context = LocalContext.current
    return remember(context) {
        ShareManagerImpl(context)
    }
}