/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import kotlin.reflect.KClass

/**
 * Returns the index of the actor in [actorClasses] that adding an actor of [newClass] replaces (the engine keeps
 * only the latest [com.pandulapeter.kubriko.actor.traits.Unique] actor of a class), or -1 when nothing is replaced.
 */
internal fun indexOfReplacedUnique(
    actorClasses: List<KClass<*>>,
    newClass: KClass<*>,
    isUnique: Boolean,
) = if (isUnique) actorClasses.indexOf(newClass) else -1
