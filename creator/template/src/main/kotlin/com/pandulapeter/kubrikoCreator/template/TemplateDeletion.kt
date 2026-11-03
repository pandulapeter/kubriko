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
 * A template version's deletion of a file the template no longer has.
 */
internal interface TemplateDeletion {

    /**
     * The file that may use this one until its own steps are done or proposed, which is when this one is deleted.
     */
    fun requiredPath(context: TemplateContext): String

    /**
     * Whether [text] is still the template's, since a file changed by hand is left alone.
     */
    fun isTemplates(text: String, context: TemplateContext): Boolean
}
