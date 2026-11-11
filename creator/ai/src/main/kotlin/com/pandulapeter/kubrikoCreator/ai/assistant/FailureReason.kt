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
 * Why a conversation of the assistant couldn't go on.
 */
public enum class FailureReason {
    /**
     * Node.js, which runs the agent through `npx`, isn't installed.
     */
    NODE_MISSING,

    /**
     * uv, which runs the agent through `uvx`, isn't installed.
     */
    UV_MISSING,

    /**
     * The agent's process couldn't be started, or ended before answering.
     */
    AGENT_NOT_STARTED,

    /**
     * The agent's process ended while it answered, so the turn ended unfinished.
     */
    AGENT_STOPPED,

    /**
     * The agent restarted without being able to load the conversation again.
     */
    CONVERSATION_LOST,

    /**
     * The agent needs the user to sign in first.
     */
    AUTH_REQUIRED,

    /**
     * The agent answered the prompt with an error.
     */
    PROMPT_FAILED,

    /**
     * The agent didn't switch to the model a plan was approved with, so the approval wasn't sent.
     */
    MODEL_NOT_SWITCHED,
}
