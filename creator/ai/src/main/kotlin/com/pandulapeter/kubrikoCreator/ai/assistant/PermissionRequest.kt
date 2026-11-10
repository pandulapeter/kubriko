/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallContent

/**
 * An agent asking whether it may run [toolCall], waiting for the user to choose one of [options].
 */
public data class PermissionRequest(
    val toolCall: SessionUpdate.ToolCallUpdate,
    val options: List<PermissionOption>,
) {
    /**
     * Whether the call edits, deletes, or moves files or runs a command through the agent's own tools rather than
     * Kubriko Creator's, so that the editor doesn't see the change and a turn's Undo can't revert it.
     */
    val isOutsideCreator: Boolean get() = toolCall.kind in PermissionHandler.changingKinds && CreatorToolName.of(toolCall.title) == null

    /**
     * The changes to files the call would make, as the agent shows them.
     */
    val diffs: List<ToolCallContent.Diff> get() = toolCall.content.orEmpty().filterIsInstance<ToolCallContent.Diff>()
}
