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

/**
 * A file that isn't text, which a template version adds as a copy of Kubriko Creator's [resource] to a project that
 * [isNeeded] says needs it and doesn't have it yet, rather than changing it step by step as an [UpgradedFile].
 */
internal class CopiedFile(
    val path: (TemplateContext) -> String,
    val resource: String,
    val isNeeded: (TemplateContext) -> Boolean,
)
