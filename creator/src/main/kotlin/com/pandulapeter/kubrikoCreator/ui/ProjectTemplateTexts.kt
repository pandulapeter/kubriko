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

import com.pandulapeter.kubrikoCreator.template.ProjectTemplate
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.template_blank_description
import kubriko.creator.generated.resources.template_blank_title
import kubriko.creator.generated.resources.template_starter_description
import kubriko.creator.generated.resources.template_starter_title
import org.jetbrains.compose.resources.StringResource

internal val ProjectTemplate.title: StringResource
    get() = when (this) {
        ProjectTemplate.STARTER -> Res.string.template_starter_title
        ProjectTemplate.BLANK -> Res.string.template_blank_title
    }

internal val ProjectTemplate.description: StringResource
    get() = when (this) {
        ProjectTemplate.STARTER -> Res.string.template_starter_description
        ProjectTemplate.BLANK -> Res.string.template_blank_description
    }
