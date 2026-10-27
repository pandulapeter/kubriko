/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creator

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager

/**
 * The opt-in contract between a game and Kubriko Creator.
 *
 * Kubriko Creator runs a project's Scenes in a separate process started from the project's own build, so it cannot
 * know how the game creates its [SerializationManager] or which Managers its Actors need. A project hands these over
 * by implementing this interface with a public class that has a no-argument constructor, and registering that class
 * as a Java service: a file named `META-INF/services/com.pandulapeter.kubriko.creator.CreatorEntryPoint` in the desktop
 * resources, containing the class's fully qualified name. Kubriko Creator generates both for the projects it manages.
 *
 * Projects that are never opened in Kubriko Creator do not need this module.
 */
interface CreatorEntryPoint {

    /**
     * Creates a new [SerializationManager] that knows every Actor type that can be placed in a Scene.
     *
     * Called once per Scene editor session. The Scene editor disposes the returned instance when it closes, so this
     * must return a new instance on every call.
     */
    fun createSerializationManager(): SerializationManager<EditableMetadata<*>, Editable<*>>

    /**
     * Creates the additional Managers that the [Editable] Actors need to work in the Scene editor (for example a
     * `SpriteManager` for Actors that draw sprites). Like [createSerializationManager], it must return new instances
     * on every call. Returns no Managers by default.
     */
    fun createCustomManagers(): List<Manager> = emptyList()
}
