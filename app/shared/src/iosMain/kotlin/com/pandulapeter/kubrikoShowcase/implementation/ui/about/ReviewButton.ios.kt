/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.UriHandler
import com.pandulapeter.kubriko.uiComponents.LargeButton
import kubriko.app.shared.generated.resources.Res
import kubriko.app.shared.generated.resources.ic_review
import kubriko.app.shared.generated.resources.other_about_write_a_review

@Composable
internal actual fun ReviewButton(uriHandler: UriHandler) = LargeButton(
    icon = Res.drawable.ic_review,
    title = Res.string.other_about_write_a_review,
    onButtonPressed = { uriHandler.openUri("https://apps.apple.com/app/id6743525729") },
)