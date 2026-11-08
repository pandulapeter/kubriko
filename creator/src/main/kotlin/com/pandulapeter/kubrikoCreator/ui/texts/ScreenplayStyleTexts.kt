/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.texts

import com.pandulapeter.kubrikoCreator.screenplay.ScreenplayStyle
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.screenplay_style_code
import kubriko.creator.generated.resources.screenplay_style_copied_number
import kubriko.creator.generated.resources.screenplay_style_history
import kubriko.creator.generated.resources.screenplay_style_long_planned_item
import kubriko.creator.generated.resources.screenplay_style_narration
import kubriko.creator.generated.resources.screenplay_style_prose_in_planned
import kubriko.creator.generated.resources.screenplay_style_too_long
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [ScreenplayStyle.Rule].
 */
internal fun ScreenplayStyle.Rule.toResource(): StringResource = when (this) {
    ScreenplayStyle.Rule.HISTORY -> Res.string.screenplay_style_history
    ScreenplayStyle.Rule.NARRATION -> Res.string.screenplay_style_narration
    ScreenplayStyle.Rule.CODE -> Res.string.screenplay_style_code
    ScreenplayStyle.Rule.COPIED_NUMBER -> Res.string.screenplay_style_copied_number
    ScreenplayStyle.Rule.LONG_PLANNED_ITEM -> Res.string.screenplay_style_long_planned_item
    ScreenplayStyle.Rule.PROSE_IN_PLANNED -> Res.string.screenplay_style_prose_in_planned
    ScreenplayStyle.Rule.TOO_LONG -> Res.string.screenplay_style_too_long
}
