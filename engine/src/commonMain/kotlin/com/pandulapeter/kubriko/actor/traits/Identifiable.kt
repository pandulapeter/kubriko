/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor

/**
 * Should be implemented by [Actor]s that want to have a name identifier.
 *
 * [Kubriko] does NOT enforce these names to be unique. If you want "singleton" [Actor]s,
 * take a look at the [Unique] trait instead.
 */
interface Identifiable : Actor {

    /**
     * The name of the [Actor] instance.
     *
     * If implementations provide `null`, [Kubriko] will set a randomly generated value
     * when the actor is added to the scene.
     */
    var name: String?
}