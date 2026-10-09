/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation

import kubriko.app.shared.generated.resources.Res
import kubriko.app.shared.generated.resources.demos
import kubriko.app.shared.generated.resources.games
import kubriko.app.shared.generated.resources.ic_demos
import kubriko.app.shared.generated.resources.ic_games
import kubriko.app.shared.generated.resources.ic_other
import kubriko.app.shared.generated.resources.ic_tests
import kubriko.app.shared.generated.resources.other
import kubriko.app.shared.generated.resources.tests
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

internal enum class ShowcaseEntryType(
    val titleStringResource: StringResource,
    val iconDrawableResource: DrawableResource,
) {
    GAME(
        titleStringResource = Res.string.games,
        iconDrawableResource = Res.drawable.ic_games,
    ),
    DEMO(
        titleStringResource = Res.string.demos,
        iconDrawableResource = Res.drawable.ic_demos,
    ),
    TEST(
        titleStringResource = Res.string.tests,
        iconDrawableResource = Res.drawable.ic_tests,
    ),
    OTHER(
        titleStringResource = Res.string.other,
        iconDrawableResource = Res.drawable.ic_other,
    ),
}
