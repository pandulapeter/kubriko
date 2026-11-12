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

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.screenplay.accentOf
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_file
import kubriko.creator.generated.resources.ic_manager_type
import kubriko.creator.generated.resources.ic_page
import kubriko.creator.generated.resources.ic_property
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_sound
import kubriko.creator.generated.resources.ic_sprite
import kubriko.creator.generated.resources.quick_open_property
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

/**
 * The icon of what [this] opens, as the sidebar shows its kind.
 */
internal val QuickOpenTarget.icon: DrawableResource
    get() = when (this) {
        is QuickOpenTarget.Property -> Res.drawable.ic_property
        is QuickOpenTarget.File -> when (mention) {
            is Mention.ActorType -> Res.drawable.ic_actor_type
            is Mention.ManagerType -> Res.drawable.ic_manager_type
            is Mention.Scene -> Res.drawable.ic_scene
            is Mention.Page -> Res.drawable.ic_page
            is Mention.Sprite -> Res.drawable.ic_sprite
            is Mention.Audio -> Res.drawable.ic_sound
            is Mention.Kotlin -> Res.drawable.ic_code
            is Mention.Document, is Mention.OtherFile -> Res.drawable.ic_file
        }
    }

/**
 * The accent of what [this] opens: a Property takes its type's.
 */
internal val QuickOpenTarget.accent: Accent?
    get() = when (this) {
        is QuickOpenTarget.Property -> if (isOfManagerType) Accent.CREW else Accent.CAST
        is QuickOpenTarget.File -> accentOf(mention)
    }

/**
 * Where [this] is, for the row showing it: a Property's type, or the folder [detail] names.
 */
@Composable
internal fun QuickOpenTarget.detailText(detail: String?): String? = when (this) {
    is QuickOpenTarget.Property -> stringResource(Res.string.quick_open_property, type)
    is QuickOpenTarget.File -> detail
}
