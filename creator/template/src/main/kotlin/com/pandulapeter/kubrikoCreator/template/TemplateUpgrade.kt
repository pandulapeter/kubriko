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
 * What it takes to bring a project's template files up to what this version of Kubriko Creator needs.
 */
public sealed interface TemplateUpgrade {

    /**
     * The project needs nothing.
     */
    public data object UpToDate : TemplateUpgrade

    /**
     * The project needs changes.
     *
     * @param changes The exact changes Kubriko Creator can make, to files that still look like the template made them.
     * @param byHand The files (relative to the project root) that were changed too much, or are missing, which the user
     *   has to change by hand.
     */
    public data class Needed(
        public val changes: List<ProposedFileChange>,
        public val byHand: List<String>,
    ) : TemplateUpgrade
}

/**
 * A change to one file, relative to the project root, shown as a diff before it is made.
 *
 * @param after The new content of the file, or null if it is deleted.
 */
public data class ProposedFileChange(
    public val path: String,
    public val before: String,
    public val after: String?,
)
