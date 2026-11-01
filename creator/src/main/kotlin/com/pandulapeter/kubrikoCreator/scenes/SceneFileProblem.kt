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
 * Why a Scene file can't be read as a Scene, so that no command can change it until it is fixed by hand.
 */
internal sealed interface SceneFileProblem {

    /**
     * The file isn't a list of Actors.
     *
     * @param reason What the parser said.
     */
    data class Unparsable(
        val reason: String,
    ) : SceneFileProblem

    /**
     * The saved state of the Actor [actorId] isn't a JSON object.
     */
    data class StateNotAnObject(
        val actorId: String,
    ) : SceneFileProblem
}
