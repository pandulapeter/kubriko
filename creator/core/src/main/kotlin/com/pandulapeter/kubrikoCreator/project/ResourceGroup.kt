/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

/**
 * The groups of the resource tree, in display order.
 */
public enum class ResourceGroup {
    SCENES,

    /**
     * The project's Markdown files outside the screenplay (its `README.md`, `AGENTS.md`, `docs/`), which aren't pages.
     */
    DOCUMENTS,
    SPRITES,
    SOUNDS,
    MUSIC,
    KOTLIN,
}
