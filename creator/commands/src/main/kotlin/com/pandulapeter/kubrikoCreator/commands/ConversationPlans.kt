/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The plans the assistant's conversations laid out with [com.pandulapeter.kubrikoCreator.commands.project.SetPlan], by
 * the ID of each one's tool address, which the assistant panel shows as the conversation's checklist. An agent that
 * reports plans of its own through ACP needs none of this; every agent can use it.
 */
public class ConversationPlans {
    private val plans = MutableStateFlow<Map<String, List<PlanStep>>>(emptyMap())

    /**
     * Keeps [steps] as the conversation [conversationId]'s plan, in place of the one it had.
     */
    public fun set(conversationId: String, steps: List<PlanStep>) {
        plans.update { it + (conversationId to steps) }
    }

    /**
     * The conversation [conversationId]'s plan, if it has one.
     */
    internal operator fun get(conversationId: String): List<PlanStep>? = plans.value[conversationId]

    /**
     * The conversation [conversationId]'s plan each time it sets one, starting with the one it has, if any.
     */
    public fun of(conversationId: String): Flow<List<PlanStep>> = plans.map { it[conversationId] }.filterNotNull().distinctUntilChanged()

    /**
     * Forgets the conversation [conversationId]'s plan, once it ended.
     */
    public fun forget(conversationId: String) {
        plans.update { it - conversationId }
    }
}

/**
 * A step of a plan, and how far it got.
 */
public data class PlanStep(
    val text: String,
    val status: PlanStepStatus,
)

@Serializable
public enum class PlanStepStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("in_progress")
    IN_PROGRESS,

    @SerialName("completed")
    COMPLETED,
}
