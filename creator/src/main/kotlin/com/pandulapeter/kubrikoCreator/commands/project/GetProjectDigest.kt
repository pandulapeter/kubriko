/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.context.DigestText
import com.pandulapeter.kubrikoCreator.context.projectDigest
import kotlinx.serialization.json.JsonPrimitive

internal object GetProjectDigest : ReadCommand<NoInput> {
    override val name = "get_project_digest"
    override val description = "Sums the whole game up in about 2,000 tokens, computed by Kubriko Creator: its Actor types and Manager types by folder, " +
        "each with its description, parent, Properties, and Events (a large project leaves these out), its Scenes with the Actors they place, and " +
        "what refers to what (who adds, collides with, reads, changes, or goes to whom). The best first call: read_actor_type, read_scene, " +
        "and get_relations take it from there."
    override val inputSerializer = NoInput.serializer()

    override suspend fun read(context: CommandContext, input: NoInput) = CommandOutcome.Success(JsonPrimitive(DigestText.of(context.projectDigest())))
}
