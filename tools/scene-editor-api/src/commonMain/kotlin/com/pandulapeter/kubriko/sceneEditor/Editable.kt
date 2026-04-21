/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.serialization.Serializable


/**
 * An interface for [Actor]s that can be manipulated within the Kubriko Scene Editor.
 *
 * This interface extends [Serializable], meaning that editable actors must also know how to
 * save and restore their state. Editable actors are required to be [Positionable] so they
 * can be moved around within the editor's workspace.
 *
 * @param T The type of the editable actor.
 */
interface Editable<T : Editable<T>> : Serializable<T>, Positionable
