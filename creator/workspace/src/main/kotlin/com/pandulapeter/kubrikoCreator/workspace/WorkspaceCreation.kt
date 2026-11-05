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
import com.pandulapeter.kubrikoCreator.commands.screenplay.CreateDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.CreatePage
import com.pandulapeter.kubrikoCreator.commands.screenplay.MoveDocument
import com.pandulapeter.kubrikoCreator.screenplay.DocumentNaming

/**
 * Creates what the sidebar names in place, each through its command, and shows it once it exists; moves a document so
 * that it stays shown wherever it goes.
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

    /**
     * Creates a document called [name] (which may name folders too, as `notes/combat`) in [folder] (an empty string for
     * the project's root), titled after its name, and shows it.
     *
     * @return Why [name] can't be used, or null if the document is being created.
     */
    public fun createDocument(folder: String, name: String): Rejection? {
        val path = DocumentNaming.pathOf(folder, name)
        return commands.dispatchIfValid(Invocation(CreateDocument, CreateDocument.Input(path, "# ${DocumentNaming.nameOf(path)}\n"))) { selection.openDocument(path) }
    }

    /**
     * Moves the document at [path] to [newPath], showing it there if it was shown, since the move takes its old path
     * away (which closes what shows it) before the move's result comes back.
     *
     * @return Why it can't be moved there, or null if it is being moved.
     */
    public fun moveDocument(path: String, newPath: String): Rejection? {
        val wasShown = selection.current.value == WorkspaceSelection.Document(path)
        return commands.dispatchIfValid(Invocation(MoveDocument, MoveDocument.Input(path, newPath))) { if (wasShown) selection.openDocument(newPath) }
    }
}
