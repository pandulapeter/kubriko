/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

/**
 * A change [SceneStore] made to the Scene files, which it records for [SceneStore.takeChanges].
 */
public sealed interface SceneChange {

    /**
     * The Scene [name] was written, as a new file when [isNew].
     */
    public data class Written(val name: String, val isNew: Boolean) : SceneChange

    /**
     * The Scene [name] was deleted.
     */
    public data class Deleted(val name: String) : SceneChange

    /**
     * The Scene [oldName] was renamed or moved to [newName].
     */
    public data class Moved(val oldName: String, val newName: String) : SceneChange

    /**
     * A folder of the Scenes folder was created or deleted.
     */
    public data object FoldersChanged : SceneChange
}
