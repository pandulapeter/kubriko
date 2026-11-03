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
 * The projects Kubriko Creator can create. Each one's files live in this module's resources under `templates/<id>`.
 */
public enum class ProjectTemplate(
    public val id: String,
) {
    STARTER(
        id = "starter",
    ),
}
