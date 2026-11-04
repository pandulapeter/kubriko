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

import com.pandulapeter.kubrikoCreator.context.RelationSubject

/**
 * A command that deletes a part of the game, cleaning up what refers to it ([deleteWithUsages]), so that what it will
 * change can be shown before it happens ([usagesOf]).
 */
public interface DeletingCommand<I> : WriteCommand<I> {

    /**
     * What the command with [input] deletes.
     */
    public fun subject(input: I): RelationSubject
}
