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

import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps secrets in memory, or refuses to keep them while [isWritable] is false.
 */
internal class FakeSecretStore(
    override val isAvailable: Boolean = true,
    var isWritable: Boolean = true,
) : SecretStore {
    val secrets = ConcurrentHashMap<String, String>()

    override fun read(name: String) = secrets[name]

    override fun write(name: String, value: String) = isWritable.also { if (it) secrets[name] = value }

    override fun delete(name: String) {
        secrets.remove(name)
    }
}
