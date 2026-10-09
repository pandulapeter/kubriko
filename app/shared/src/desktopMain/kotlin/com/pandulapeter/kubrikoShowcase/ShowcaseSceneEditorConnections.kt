/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase

import com.pandulapeter.kubriko.shared.SceneEditorConnection
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry

/**
 * The [SceneEditorConnection]s of the examples with a scene editor, for the desktop shell's editor windows. They are the
 * ones the Showcase's session hands to the examples' state holders, and they live as long as the process.
 */
object ShowcaseSceneEditorConnections {
    val annoyedPenguins get() = showcaseSession.sceneEditorConnectionFor(ShowcaseEntry.ANNOYED_PENGUINS)
    val blockysJourney get() = showcaseSession.sceneEditorConnectionFor(ShowcaseEntry.BLOCKYS_JOURNEY)
    val performance get() = showcaseSession.sceneEditorConnectionFor(ShowcaseEntry.PERFORMANCE)
    val physics get() = showcaseSession.sceneEditorConnectionFor(ShowcaseEntry.PHYSICS)
}
