/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import com.pandulapeter.kubrikoCreator.screenplay.MovedFiles
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class MarkdownIdentityTest {

    @Test
    fun `a page keeps its identity where Kubriko Creator moved it, and only there`() {
        val moved = MovedFiles()
        val identity = MarkdownIdentity(moved)
        val hero = identity.of(WorkspaceSelection.Page("hero"))
        assertSame(hero, identity.of(WorkspaceSelection.Page("hero")))
        moved.record("screenplay/hero.md", "screenplay/cast/hero.md")
        assertSame(hero, identity.of(WorkspaceSelection.Page("cast/hero")))
        moved.record("screenplay/cast/hero.md", "docs/hero.md")
        assertSame(hero, identity.of(WorkspaceSelection.Document("docs/hero.md")))
        val villain = identity.of(WorkspaceSelection.Page("villain"))
        assertNotSame(hero, villain)
        assertNotSame(villain, identity.of(WorkspaceSelection.None))
        assertNotSame(hero, identity.of(WorkspaceSelection.Document("docs/hero.md")))
    }
}
