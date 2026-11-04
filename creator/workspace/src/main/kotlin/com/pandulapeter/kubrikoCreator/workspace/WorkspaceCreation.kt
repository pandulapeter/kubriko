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
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.CreateActorType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.CreateManagerType
import com.pandulapeter.kubrikoCreator.commands.scenes.CreateScene

/**
 * Creates an empty Actor type and selects it, closing the Scene shown.
 *
 * @return Why [name] can't be used, or null if the Actor type is being created.
 */
public fun WorkspaceController.createActorType(name: String): Rejection? {
    val invocation = Invocation(CreateActorType, ActorTypeDefinition(name = name))
    commands.validate(invocation)?.let { return it }
    commands.dispatch(invocation) { selection.selectType(name) }
    return null
}

/**
 * Creates an empty Manager type and selects it, closing the Scene shown.
 *
 * @return Why [name] can't be used, or null if the Manager type is being created.
 */
public fun WorkspaceController.createManagerType(name: String): Rejection? {
    val invocation = Invocation(CreateManagerType, ManagerTypeDefinition(name = name))
    commands.validate(invocation)?.let { return it }
    commands.dispatch(invocation) { selection.selectType(name) }
    return null
}

/**
 * Creates an empty Scene.
 *
 * @return Why [name] can't be used, or null if the Scene is being created.
 */
public fun WorkspaceController.createScene(name: String): Rejection? = dispatchIfValid(Invocation(CreateScene, CreateScene.Input(name)))
