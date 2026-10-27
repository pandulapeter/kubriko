/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.template_starter_description
import kubriko.creator.generated.resources.template_starter_title
import org.jetbrains.compose.resources.StringResource

/**
 * The projects Kubriko Creator can create. Each one's files live in the app's resources under `templates/<id>`.
 */
internal enum class ProjectTemplate(
    val id: String,
    val title: StringResource,
    val description: StringResource,
) {
    STARTER(
        id = "starter",
        title = Res.string.template_starter_title,
        description = Res.string.template_starter_description,
    ),
}
