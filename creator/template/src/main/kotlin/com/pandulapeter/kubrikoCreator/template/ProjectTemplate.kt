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
 * The projects Kubriko Creator can create. Each one's files live in this module's resources under `templates/<id>`; one
 * made from [base] lists there only the files it replaces (`files.txt`) and those of the base it leaves out
 * (`removed.txt`), so that the files they share, and their upgrades, live in one place.
 */
public enum class ProjectTemplate(
    public val id: String,
    public val base: ProjectTemplate? = null,
) {
    STARTER(
        id = "starter",
    ),

    /**
     * The starter without its Ball: one empty Scene, for a game the writers' room designs from an idea.
     */
    BLANK(
        id = "blank",
        base = STARTER,
    ),
}
