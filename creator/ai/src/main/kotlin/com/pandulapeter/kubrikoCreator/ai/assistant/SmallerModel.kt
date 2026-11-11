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

import com.agentclientprotocol.model.SessionConfigId
import com.agentclientprotocol.model.SessionConfigOption
import com.agentclientprotocol.model.SessionConfigOptionCategory
import com.agentclientprotocol.model.SessionConfigSelectOption
import com.agentclientprotocol.model.SessionConfigSelectOptions
import com.agentclientprotocol.model.SessionConfigValueId

/**
 * A model of the conversation's agent that costs less than the one it uses, which a plan can be approved with: small edits
 * rarely need the largest model, and the user, who pays, decides.
 *
 * @param option The agent's model option.
 * @param value The smaller model's value of it.
 * @param name The smaller model's name, as the agent gives it.
 */
public data class SmallerModel(
    val option: SessionConfigId,
    val value: SessionConfigValueId,
    val name: String,
) {
    public companion object {

        /**
         * Models by tier, the smaller first, within each family whose order is known. A model of no known tier (one of
         * another vendor, or a new family) is offered nothing, since a wrong guess would cost the user more, or less work.
         */
        private val families = listOf(
            listOf("haiku", "sonnet", "opus"),
            listOf("flash", "pro"),
        )

        /**
         * The model a tier below the one the conversation uses, among the agent's [options], or null if the agent offers
         * no model option, the current model's tier isn't known, or nothing is a tier below it. The current model's tier
         * is read from its description too, since an agent's default may name the model it stands for only there.
         */
        public fun of(options: List<SessionConfigOption>): SmallerModel? {
            val option = options.filterIsInstance<SessionConfigOption.Select>().firstOrNull { it.category == SessionConfigOptionCategory.MODEL } ?: return null
            val choices = option.options.flattened()
            val current = choices.firstOrNull { it.value == option.currentValue } ?: return null
            val (family, tier) = tierOf("${current.value.value} ${current.name} ${current.description.orEmpty()}") ?: return null
            if (tier == 0) return null
            val smaller = choices.firstOrNull { tierOf("${it.value.value} ${it.name}") == family to tier - 1 } ?: return null
            return SmallerModel(option.id, smaller.value, smaller.name)
        }

        private fun tierOf(text: String): Pair<Int, Int>? {
            val words = text.lowercase().split(Regex("[^a-z]+")).toSet()
            families.forEachIndexed { family, tiers ->
                tiers.indexOfLast { it in words }.takeIf { it >= 0 }?.let { return family to it }
            }
            return null
        }

        private fun SessionConfigSelectOptions.flattened(): List<SessionConfigSelectOption> = when (this) {
            is SessionConfigSelectOptions.Flat -> options
            is SessionConfigSelectOptions.Grouped -> groups.flatMap { it.options }
        }
    }
}
