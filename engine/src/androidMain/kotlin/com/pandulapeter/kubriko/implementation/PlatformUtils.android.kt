/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import com.pandulapeter.kubriko.manager.MetadataManager

internal actual fun getDefaultFocusDebounce() = 350L

internal actual fun getPlatform(): MetadataManager.Platform = MetadataManager.Platform.Android(
    androidSdkVersion = Build.VERSION.SDK_INT,
)

internal actual val activeLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED

internal actual val shouldUseLifecycleFocus = true

@Composable
internal actual fun PlatformFocusEffect(onFocusChanged: (Boolean) -> Unit) = Unit
