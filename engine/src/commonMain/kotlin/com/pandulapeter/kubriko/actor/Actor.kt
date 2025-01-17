/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager

/**
 * Marker interface that should be implemented by all classes handled by [ActorManager].
 */
interface Actor {

    // TODO: Documentation
    fun onAdded(kubriko: Kubriko) = Unit

    // TODO: Documentation
    fun onRemoved() = Unit
}