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
         * is read from its description only when its name doesn't say it, since an agent's default may name the model it
         * stands for only there, while another's description may name a larger one it isn't.
         *
         * An agent may list several models of that tier, older ones and variants among them, so the one offered is the
         * family's plain model (Flash, not Flash-Lite; Sonnet, not Sonnet with a longer context), then one of the current
         * model's version (Sonnet 5.5 for Opus 5.5), then the one the agent names by the tier alone (`sonnet`, its alias
         * for the newest), and then the first the agent lists.
         */
        public fun of(options: List<SessionConfigOption>): SmallerModel? {
            val option = options.filterIsInstance<SessionConfigOption.Select>().firstOrNull { it.category == SessionConfigOptionCategory.MODEL } ?: return null
            val choices = option.choices()
            val current = choices.firstOrNull { it.value == option.currentValue } ?: return null
            val currentText = if (tierOf(current) == null) current.description.orEmpty() else "${current.value.value} ${current.name}"
            val (family, tier) = tierOf(currentText) ?: return null
            if (tier == 0) return null
            val tierName = families[family][tier - 1]
            val currentWords = wordsOf(currentText)
            val version = versionOf(current)
            val smaller = choices.withIndex()
                .filter { (_, choice) -> choice.value != current.value && tierOf(choice) == family to tier - 1 }
                .minWithOrNull(
                    compareBy<IndexedValue<SessionConfigSelectOption>> { (_, choice) -> (wordsOf(choice.name) - currentWords - families[family].toSet()).size }
                        .thenBy { (_, choice) -> if (version != null && versionOf(choice) == version) 0 else 1 }
                        .thenBy { (_, choice) -> if (choice.value.value.equals(tierName, ignoreCase = true)) 0 else 1 }
                        .thenBy { it.index },
                )?.value ?: return null
            return SmallerModel(option.id, smaller.value, smaller.name)
        }

        private fun tierOf(choice: SessionConfigSelectOption) = tierOf("${choice.value.value} ${choice.name}")

        private fun tierOf(text: String): Pair<Int, Int>? {
            val words = wordsOf(text)
            families.forEachIndexed { family, tiers ->
                tiers.indexOfLast { it in words }.takeIf { it >= 0 }?.let { return family to it }
            }
            return null
        }

        private fun wordsOf(text: String) = text.lowercase().split(nonLetters).filter { it.isNotEmpty() }.toSet()

        /**
         * The version [choice] names, from its name, its description, or its value (where dashes stand for dots, as in
         * `claude-sonnet-4-5`), the first that names one; a number before letters (a 1M context) isn't one.
         */
        private fun versionOf(choice: SessionConfigSelectOption) = versionIn(choice.name)
            ?: choice.description?.let(::versionIn)
            ?: versionIn(choice.value.value.replace(versionDash, "."))

        private fun versionIn(text: String) = version.find(text)?.value

        private val nonLetters = Regex("[^a-z]+")
        private val version = Regex("""(?<![\w.])\d+(\.\d+)*(?![\w.])""")
        private val versionDash = Regex("""(?<=\d)-(?=\d)""")
    }
}
