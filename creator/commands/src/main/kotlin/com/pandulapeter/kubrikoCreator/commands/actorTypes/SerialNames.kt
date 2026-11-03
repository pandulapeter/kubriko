/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.commands.JsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The names Events, Blocks, and kinds of Property have in definitions and tool inputs (`everyFrame`, `setVelocity`, `number`).
 */
internal val EventType.serialName get() = EventType.serializer().descriptor.getElementName(ordinal)

internal val BlockDefinition.serialName get() = Json.encodeToJsonElement(BlockDefinition.serializer(), this).jsonObject.getValue(JsonSchema.TYPE_PROPERTY).jsonPrimitive.content

internal val BlockKind.serialName get() = createDefault().serialName

internal val PropertyKind.serialName get() = Json.encodeToJsonElement(PropertyDefinition.serializer(), create("name")).jsonObject.getValue(JsonSchema.TYPE_PROPERTY).jsonPrimitive.content
