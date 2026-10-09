/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared.ui

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kubriko.examples.shared.generated.resources.Res
import kubriko.examples.shared.generated.resources.shaders_not_supported

/**
 * Whether the string of [ShadersNotSupportedMessage] is preloaded, for the resource gates of the examples using it.
 */
@Composable
fun areShadersNotSupportedMessageResourcesLoaded() = preloadedString(Res.string.shaders_not_supported).value.isNotBlank()
