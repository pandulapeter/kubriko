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
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup

/**
 * What commands know of the project's files from the last resource scan, so that validating reads nothing.
 *
 * @param audioFiles The sounds and music of the project, or null if they aren't known, so they aren't checked.
 * @param spriteFiles The images Actor types can be drawn as, or null if they aren't known, so they aren't checked.
 * @param resources Every file the scan found, by group, or null before the first scan: what the sprite, sound, and
 *   music commands check against, since [spriteFiles] leaves out images whose names make no resource accessor, which
 *   are the ones most in need of a rename.
 */
public class ScannedFiles(
    public val audioFiles: () -> AudioFiles? = { null },
    public val spriteFiles: () -> SpriteFiles? = { null },
    public val resources: () -> Map<ResourceGroup, List<ProjectResource>>? = { null },
) {
    public companion object {

        /**
         * What [resources] (the last scan's files, by group, empty before the first scan) knows, read anew every time.
         */
        public fun of(resources: () -> Map<ResourceGroup, List<ProjectResource>>): ScannedFiles = ScannedFiles(
            audioFiles = { AudioFiles.ofScanned(resources()) },
            spriteFiles = { SpriteFiles.of(resources()) },
            resources = { resources().takeIf { it.isNotEmpty() } },
        )
    }
}
