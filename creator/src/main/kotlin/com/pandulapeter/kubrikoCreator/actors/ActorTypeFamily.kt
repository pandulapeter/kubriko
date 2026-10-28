/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * How a project's Actor types inherit from each other through their `parent`, like GameMaker's parent objects: a child
 * gets its ancestors' Properties and Events, and replaces an Event of theirs with one of its own of the same kind.
 *
 * Built from every Actor type of the project; a `parent` naming no Actor type, or making a cycle, counts as none.
 */
internal class ActorTypeFamily(actorTypes: List<ActorTypeDefinition>) {
    private val byName = actorTypes.associateBy { it.name }
    private val parents: Map<String, String> = actorTypes.associate { it.name to it.parent }.let { declaredParents ->
        actorTypes.mapNotNull { type ->
            type.parent?.takeIf { it in byName && !isAncestor(type.name, it, declaredParents) }?.let { type.name to it }
        }.toMap()
    }
    private val parentNames = parents.values.toSet()

    fun parentOf(name: String) = parents[name]

    /**
     * The ancestors of [name], its parent first.
     */
    fun ancestorsOf(name: String): List<String> = generateSequence(parents[name]) { parents[it] }.toList()

    fun childrenOf(name: String) = parents.filterValues { it == name }.keys.sorted()

    /**
     * Every Actor type that inherits from [name], however far down.
     */
    fun descendantsOf(name: String): List<String> = childrenOf(name).flatMap { listOf(it) + descendantsOf(it) }

    fun hasChildren(name: String) = name in parentNames

    /**
     * [name] and every Actor type that inherits from it: what a reference to [name] stands for.
     */
    fun familyOf(name: String) = listOf(name) + descendantsOf(name)

    /**
     * The topmost ancestor of [name], or [name] itself.
     */
    fun rootOf(name: String) = ancestorsOf(name).lastOrNull() ?: name

    /**
     * The Properties of [name]: its ancestors' (the topmost one's first), with the initial values the nearest one that
     * names them again gives them, then its own new ones.
     */
    fun propertiesOf(name: String): List<PropertyDefinition> {
        val chain = (ancestorsOf(name).reversed() + name).mapNotNull(byName::get)
        val properties = LinkedHashMap<String, PropertyDefinition>()
        chain.forEach { type ->
            type.properties.forEach { property ->
                val inherited = properties[property.name]
                if (inherited == null || PropertyKind.of(inherited) == PropertyKind.of(property)) properties[property.name] = property
            }
        }
        return properties.values.toList()
    }

    /**
     * The Events [name]'s Actors react to: its own, and each of its ancestors' that none nearer to it replaces.
     */
    fun eventsOf(name: String): List<EventDefinition> {
        val own = byName[name]?.events.orEmpty()
        val inherited = parents[name]?.let(::eventsOf).orEmpty().filter { event -> own.none { it.replaces(event) } }
        return inherited + own
    }

    companion object {

        /**
         * Whether [ancestor] is [name] or one of its ancestors, going by [parents] (each type's parent, if any).
         */
        fun isAncestor(ancestor: String, name: String, parents: Map<String, String?>): Boolean {
            val visited = mutableSetOf<String>()
            var current: String? = name
            while (current != null && visited.add(current)) {
                if (current == ancestor) return true
                current = parents[current]
            }
            return false
        }

        /**
         * Whether [this] Event takes the place of [other], an ancestor's: the same kind, for the same key, other Actor
         * type, or Timer.
         */
        fun EventDefinition.replaces(other: EventDefinition) = type == other.type && key == other.key && otherActorType == other.otherActorType && timer == other.timer
    }
}
