/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles

/**
 * What commands know of the project's files from the last resource scan, so that validating reads nothing.
 *
 * @param audioFiles The sounds and music of the project, or null if they aren't known, so they aren't checked.
 * @param spriteFiles The images Actor types can be drawn as, or null if they aren't known, so they aren't checked.
 * @param documents The paths of the project's Markdown files that aren't pages.
 */
public class ScannedFiles(
    public val audioFiles: () -> AudioFiles? = { null },
    public val spriteFiles: () -> SpriteFiles? = { null },
    public val documents: () -> List<String> = { emptyList() },
)
