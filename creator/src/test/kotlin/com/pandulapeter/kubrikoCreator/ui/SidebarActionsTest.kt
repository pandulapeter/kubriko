/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SidebarActionsTest {

    @Test
    fun aFolderShownEmptyIsDeletedAloneSoWhatWasPutInItSinceIsntTakenWithoutAsking() {
        assertTrue(folderDeleteOf(FolderGroup.SCENES, "levels", isEmpty = true).input.keepsContents)
        assertFalse(folderDeleteOf(FolderGroup.SCENES, "levels", isEmpty = false).input.keepsContents)
    }
}
