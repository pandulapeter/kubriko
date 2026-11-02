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
 * One template version's change to one file.
 */
internal interface TemplateStep {
    val version: Int

    /**
     * Whether [text] already has this step's change, whoever made it.
     */
    fun isDone(text: String, context: TemplateContext): Boolean

    /**
     * [text] with this step made, or null if the file doesn't look like the template's (left to the user).
     */
    fun change(text: String, context: TemplateContext): String?
}
