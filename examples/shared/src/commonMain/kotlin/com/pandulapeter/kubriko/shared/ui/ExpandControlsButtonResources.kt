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
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kubriko.examples.shared.generated.resources.Res
import kubriko.examples.shared.generated.resources.collapse_controls
import kubriko.examples.shared.generated.resources.expand_controls
import kubriko.examples.shared.generated.resources.ic_brush

/**
 * Whether the icon and strings of [ExpandControlsButton] are preloaded, for the resource gates of the examples using it.
 */
@Composable
fun areExpandControlsButtonResourcesLoaded() = preloadedImageVector(Res.drawable.ic_brush).value != null
        && preloadedString(Res.string.expand_controls).value.isNotBlank()
        && preloadedString(Res.string.collapse_controls).value.isNotBlank()
