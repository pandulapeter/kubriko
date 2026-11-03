/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * What makes an Event happen: its type and the one detail that type has. Two Events with the same trigger run at the
 * same moment, and a child's replaces its parent's. Not an identity: a type may have several Events with one trigger,
 * which run in order.
 */
public data class EventTrigger(
    val type: EventType,
    val key: KeyChoice?,
    val otherActorType: String?,
    val timer: String?,
)
