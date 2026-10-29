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

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Who an agent works as, as it reports itself with `_auth/status_update` (Claude Agent's extension, sent at start and
 * whenever the identity changes).
 *
 * @param label The agent's own name for it, such as "Claude Max" or "Anthropic API key".
 * @param detail What tells it apart, such as where an API key comes from.
 */
internal data class AuthStatus(
    val kind: Kind,
    val label: String,
    val detail: String? = null,
    val email: String? = null,
) {
    val isSignedIn get() = kind != Kind.NONE

    enum class Kind {
        NONE,

        /**
         * A subscription, signed in with Claude.ai.
         */
        ACCOUNT,
        API_KEY,
        GATEWAY,

        /**
         * A cloud provider's credentials, such as Amazon Bedrock's.
         */
        EXTERNAL,

        /**
         * A kind this version of Kubriko Creator doesn't know, which still counts as signed in.
         */
        OTHER,
    }

    companion object {
        const val METHOD = "_auth/status_update"

        /**
         * The status in the `params` of an `_auth/status_update` notification, or null if they don't hold one.
         */
        fun parse(params: JsonElement?): AuthStatus? {
            val status = (params as? JsonObject)?.get("authStatus") as? JsonObject ?: return null
            val kind = status.string("kind") ?: return null
            return AuthStatus(
                kind = when (kind) {
                    "none" -> Kind.NONE
                    "account" -> Kind.ACCOUNT
                    "api_key" -> Kind.API_KEY
                    "gateway" -> Kind.GATEWAY
                    "external" -> Kind.EXTERNAL
                    else -> Kind.OTHER
                },
                label = status.string("label") ?: kind,
                detail = status.string("detail"),
                email = (status["account"] as? JsonObject)?.string("email"),
            )
        }

        private fun JsonObject.string(key: String) = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    }
}
