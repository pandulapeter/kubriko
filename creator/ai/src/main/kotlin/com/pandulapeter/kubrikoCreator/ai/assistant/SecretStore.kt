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

/**
 * Secrets, such as an API key, kept outside Kubriko Creator's settings files, by name. Calls block.
 */
public interface SecretStore {

    /**
     * Whether this computer has somewhere to keep secrets.
     */
    public val isAvailable: Boolean

    /**
     * The secret kept under [name], if there is one.
     */
    public fun read(name: String): String?

    /**
     * @return Whether it was kept.
     */
    public fun write(name: String, value: String): Boolean

    /**
     * Forgets the secret kept under [name], if there is one.
     */
    public fun delete(name: String)
}
