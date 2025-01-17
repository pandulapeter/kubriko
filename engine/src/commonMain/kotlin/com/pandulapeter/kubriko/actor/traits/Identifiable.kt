/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor

/**
 * Should be implemented by [Actor]s that want to have a [name] set for their instances.
 * [Kubriko] does NOT enforce these [name]-s to be unique. If you want "singleton" [Actor]s, take a look at the [Unique] trait instead.
 * [Identifiable] is meant for [Actor]s that want to be referenced by other classes in a more abstract way.
 * Searching for an [Actor] by name might return multiple results.
 */
// TODO: Is this really necessary?
interface Identifiable : Actor {

    /**
     * The name of the [Actor] instance.
     * If implementations provide a {null} [name], [Kubriko] will set a random-generated value when the [Actor] is added to the Scene.
     * [Kubriko] will never change the value of [name] at runtime.
     */
    var name: String?
}