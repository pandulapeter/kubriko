/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.CreateActorType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.CreateManagerType
import com.pandulapeter.kubrikoCreator.commands.scenes.CreateScene
import com.pandulapeter.kubrikoCreator.commands.screenplay.CreatePage

/**
 * Creates what the sidebar names in place, each through its command, and shows it once it exists.
 */
public class WorkspaceCreation internal constructor(
    private val commands: CommandExecutor,
    private val selection: SelectionController,
) {

    /**
     * Creates an empty Actor type and selects it, closing the Scene shown.
     *
     * @return Why [name] can't be used, or null if the Actor type is being created.
     */
    public fun createActorType(name: String): Rejection? = commands.dispatchIfValid(Invocation(CreateActorType, ActorTypeDefinition(name = name))) { selection.selectType(name) }

    /**
     * Creates an empty Manager type and selects it, closing the Scene shown.
     *
     * @return Why [name] can't be used, or null if the Manager type is being created.
     */
    public fun createManagerType(name: String): Rejection? = commands.dispatchIfValid(Invocation(CreateManagerType, ManagerTypeDefinition(name = name))) { selection.selectType(name) }

    /**
     * Creates an empty Scene.
     *
     * @return Why [name] can't be used, or null if the Scene is being created.
     */
    public fun createScene(name: String): Rejection? = commands.dispatchIfValid(Invocation(CreateScene, CreateScene.Input(name)))

    /**
     * Creates a page of the screenplay titled after its name, and shows it.
     *
     * @return Why [name] can't be used, or null if the page is being created.
     */
    public fun createPage(name: String): Rejection? =
        commands.dispatchIfValid(Invocation(CreatePage, CreatePage.Input(name, "# ${name.substringAfterLast('/')}\n"))) { selection.openPage(name) }
}
