/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges

/**
 * What a [BlockTypesController] reports while it writes, reads, and regenerates the types, called on its scope's thread.
 * Saving and failing to save have to be handled; the rest do nothing unless overridden.
 */
public interface BlockTypesListener {

    /** Called after each batch, with whether the types changed, rather than only the Scenes they were regenerated for. */
    public fun onSaved(haveTypesChanged: Boolean)

    /** Called with what a batch couldn't write, once until the failure changes. */
    public fun onSaveFailed(failure: FailedWrite)

    /** Called when a batch succeeds after one failed. */
    public fun onSavedAgain() {}

    /** Called for each definition file found unreadable, once until it changes. */
    public fun onFileUnreadable(file: UnreadableBlockTypeFile) {}

    /** Called once the types are first loaded. */
    public fun onFirstLoaded(types: List<BlockTypeDefinition>) {}

    /** Called when no definition file is unreadable any more. */
    public fun onFilesReadableAgain() {}

    /** Called with the generated files (by path) a regeneration deleted, since their types no longer exist. */
    public fun onLeftoversRemoved(paths: List<String>) {}

    /**
     * Called for each generated file (by path) whose type no longer exists but which was changed by hand, so it was kept,
     * once until it is gone.
     */
    public fun onLeftoverChangedByHand(path: String) {}

    /** Called with what syncing the build files after a batch changed. */
    public fun onBuildFilesSynced(changes: KubrikoBuildFileChanges) {}
}
