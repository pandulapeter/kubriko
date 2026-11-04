/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_android
import kubriko.creator.generated.resources.ic_apple
import kubriko.creator.generated.resources.ic_desktop
import kubriko.creator.generated.resources.ic_web
import kubriko.creator.generated.resources.play_target_android
import kubriko.creator.generated.resources.play_target_desktop
import kubriko.creator.generated.resources.play_target_ios
import kubriko.creator.generated.resources.play_target_web
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * A platform whose own source sets the sidebar's Kotlin files are grouped by, named and drawn as the Play menu does.
 */
internal enum class SourcePlatform(
    val title: StringResource,
    val icon: DrawableResource,
) {
    DESKTOP(Res.string.play_target_desktop, Res.drawable.ic_desktop),
    WEB(Res.string.play_target_web, Res.drawable.ic_web),
    ANDROID(Res.string.play_target_android, Res.drawable.ic_android),
    IOS(Res.string.play_target_ios, Res.drawable.ic_apple),
}
