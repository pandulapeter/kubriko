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
 * What the assistant keeps on this computer.
 *
 * @param settings The assistant's settings: the agent chosen, its configuration, and the like.
 * @param secrets Where the API key and agents' variables are kept.
 * @param conversations Where the conversations are kept per project, to be continued later, or null to keep none.
 * @param errorLogFile Where agents' standard error is kept ([AgentErrorLog]), for the user to open.
 */
public class AssistantStorage(
    internal val settings: AssistantSettingsStore,
    internal val secrets: SecretStore,
    internal val conversations: SavedConversationStore? = null,
    internal val errorLogFile: File? = null,
) {
    public companion object {
        /**
         * The files in Creator's settings folder and the operating system's secret store, with agents' error log at
         * [errorLogFile].
         */
        public fun default(errorLogFile: File?): AssistantStorage = AssistantStorage(
            settings = AssistantSettingsStore.default(),
            secrets = SystemSecretStore(),
            conversations = SavedConversationStore.default(),
            errorLogFile = errorLogFile,
        )
    }
}
