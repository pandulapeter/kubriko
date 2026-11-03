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

import java.io.File

/**
 * Where agents come from; all null for Claude Agent alone.
 *
 * @param registry Where the registry's agents come from.
 * @param installer Downloads the agents the registry distributes as archives.
 * @param node Downloads a Node.js of Creator's own for a user without one.
 */
public class AgentSources(
    internal val registry: AgentRegistryStore? = null,
    internal val installer: BinaryAgentInstaller? = null,
    internal val node: NodeRuntime? = null,
) {
    public companion object {
        /**
         * The ACP Registry, with archives and Node.js downloaded into Creator's agents folder.
         */
        public fun default(): AgentSources {
            val installer = BinaryAgentInstaller.default()
            return AgentSources(
                registry = AgentRegistryStore.default(),
                installer = installer,
                node = NodeRuntime(installer, File(BinaryAgentInstaller.defaultDirectory(), NodeRuntime.NODE_ID)),
            )
        }
    }
}
