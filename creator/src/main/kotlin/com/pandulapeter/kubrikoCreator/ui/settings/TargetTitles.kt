/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import com.pandulapeter.kubrikoCreator.template.GameTarget
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.target_android
import kubriko.creator.generated.resources.target_desktop
import kubriko.creator.generated.resources.target_ios
import kubriko.creator.generated.resources.target_web
import org.jetbrains.compose.resources.StringResource

/**
 * The name the user is shown for this [GameTarget].
 */
internal val GameTarget.title: StringResource
    get() = when (this) {
        GameTarget.DESKTOP -> Res.string.target_desktop
        GameTarget.WEB -> Res.string.target_web
        GameTarget.ANDROID -> Res.string.target_android
        GameTarget.IOS -> Res.string.target_ios
    }
