/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.lifecycle

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.testFixtures.CountingActor
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.fail

/**
 * Records the lifecycle callbacks of [LoggingActor]s and checks them against the documented actor lifecycle contract.
 *
 * `ADDED`, `DISPOSED` and `REMOVED` events are kept per actor. `UPDATE` events are far too many to keep under stress, so
 * each one is checked against its actor's callbacks the moment it is recorded (invariant 3) and only its tick is
 * remembered (invariant 4).
 */
class LifecycleLog(private val seed: Int) {

    enum class Kind { ADDED, UPDATE, DISPOSED, REMOVED }

    data class Event(val sequence: Long, val actorId: Int, val kind: Kind, val tick: Int, val thread: String)

    /** Incremented by the driver before each `tick(16)`. */
    @Volatile
    var currentTick = 0

    private val sequence = AtomicLong()
    private val eventsByActor = ConcurrentHashMap<Int, MutableList<Event>>()
    private val updateViolations = CopyOnWriteArrayList<String>()
    private val maximumUpdateTick = AtomicInteger(-1)

    /** The number of events of any kind recorded so far. */
    val eventCount get() = sequence.get()

    /** The highest tick any update was recorded in. */
    val highestUpdateTick get() = maximumUpdateTick.get()

    fun record(actor: LoggingActor, kind: Kind) {
        val tick = currentTick
        synchronized(actor) {
            val event = Event(sequence.incrementAndGet(), actor.id, kind, tick, Thread.currentThread().name)
            when (kind) {
                Kind.UPDATE -> {
                    maximumUpdateTick.accumulateAndGet(tick, ::maxOf)
                    if (actor.lastUpdateTick == tick) actor.updatesInLastTick++ else {
                        actor.lastUpdateTick = tick
                        actor.updatesInLastTick = 1
                    }
                    val lastRemoval = actor.lastRemovalTick
                    when {
                        actor.addedEvents == 0 -> updateViolations.add("${prefix(tick)} actor=${actor.id}: updated before it was ever added.")
                        actor.addedEvents == actor.removedEvents && lastRemoval != null && lastRemoval < tick ->
                            updateViolations.add("${prefix(tick)} actor=${actor.id}: updated after its removal in tick $lastRemoval.")
                    }
                }

                Kind.ADDED -> {
                    actor.addedEvents++
                    eventsOf(actor.id).add(event)
                }

                Kind.DISPOSED -> eventsOf(actor.id).add(event)

                Kind.REMOVED -> {
                    actor.removedEvents++
                    actor.lastRemovalTick = tick
                    eventsOf(actor.id).add(event)
                }
            }
        }
    }

    private fun eventsOf(actorId: Int) = eventsByActor.getOrPut(actorId) { mutableListOf() }

    private fun prefix(tick: Int = currentTick) = "seed=$seed tick=$tick"

    private fun eventsOf(actor: LoggingActor) = synchronized(actor) { eventsOf(actor.id).toList() }

    /** How many `DISPOSED` and `REMOVED` events [actor] has, in that order. */
    fun removalCounts(actor: LoggingActor) = eventsOf(actor).let { events ->
        events.count { it.kind == Kind.DISPOSED } to events.count { it.kind == Kind.REMOVED }
    }

    /**
     * Invariant 1: the callbacks of every actor alternate `ADDED`, `DISPOSED`, `REMOVED`. With [allowTrailingDispose],
     * an actor may end on one extra `DISPOSED` (the one `Kubriko.dispose()` sends to the actors still in the scene).
     */
    fun checkPairing(actors: Collection<LoggingActor>, allowTrailingDispose: Boolean = false) {
        for (actor in actors) {
            val events = eventsOf(actor)
            var state = Kind.REMOVED
            for (event in events) {
                val expectedPrevious = when (event.kind) {
                    Kind.ADDED -> Kind.REMOVED
                    Kind.DISPOSED -> Kind.ADDED
                    Kind.REMOVED -> Kind.DISPOSED
                    Kind.UPDATE -> error("Updates are not kept.")
                }
                if (state != expectedPrevious) {
                    fail("${prefix(event.tick)} actor=${actor.id}: ${event.kind} after $state in $events")
                }
                state = event.kind
            }
            if (state == Kind.DISPOSED && !allowTrailingDispose) {
                fail("${prefix()} actor=${actor.id}: disposed but never removed in $events")
            }
        }
    }

    /**
     * Invariants 1 to 3 at a quiescent point: pairing, membership against the sequential model and no update outside
     * membership. [liveMembers] is `allActors.value`; everything but [LoggingActor]s (the fixtures' sentinels and
     * blockers) is ignored.
     */
    fun checkInvariants(actors: Collection<LoggingActor>, expectedMembers: Set<Int>, liveMembers: List<Actor>) {
        checkPairing(actors)
        val identities = IdentityHashMap<Actor, Unit>()
        for (member in liveMembers) {
            if (identities.put(member, Unit) != null) fail("${prefix()} $member is in allActors more than once.")
        }
        val liveIds = liveMembers.filterIsInstance<LoggingActor>().map { it.id }.toSet()
        if (liveIds != expectedMembers) {
            fail("${prefix()} allActors ${liveIds.sorted()} differs from the model ${expectedMembers.sorted()}: missing ${(expectedMembers - liveIds).sorted()}, unexpected ${(liveIds - expectedMembers).sorted()}")
        }
        for (actor in actors) {
            val isPresentByCallbacks = synchronized(actor) { actor.addedEvents - actor.removedEvents } == 1
            if (isPresentByCallbacks != (actor.id in expectedMembers)) {
                fail("${prefix()} actor=${actor.id}: callbacks say present=$isPresentByCallbacks, the model says ${actor.id in expectedMembers} in ${eventsOf(actor)}")
            }
        }
        checkUpdates()
    }

    /** Invariant 3, as recorded while the updates ran. */
    fun checkUpdates() {
        if (updateViolations.isNotEmpty()) fail(updateViolations.take(10).joinToString("\n"))
    }

    /** Invariant 4: in [tick], which issued no operation, every member was updated exactly once and nothing else was. */
    fun checkSingleUpdate(actors: Collection<LoggingActor>, expectedMembers: Set<Int>, tick: Int) {
        for (actor in actors) {
            val updates = synchronized(actor) { if (actor.lastUpdateTick == tick) actor.updatesInLastTick else 0 }
            val expectedUpdates = if (actor.id in expectedMembers) 1 else 0
            if (updates != expectedUpdates) fail("${prefix(tick)} actor=${actor.id}: updated $updates times instead of $expectedUpdates.")
        }
    }
}

/**
 * A [CountingActor] that records its callbacks into a [LifecycleLog]. It is not `Positionable`, so it is always active.
 */
open class LoggingActor(
    val id: Int,
    private val log: LifecycleLog,
) : CountingActor() {
    internal var addedEvents = 0
    internal var removedEvents = 0
    internal var lastRemovalTick: Int? = null
    internal var lastUpdateTick = -1
    internal var updatesInLastTick = 0

    override fun onAdded(kubriko: Kubriko) {
        super.onAdded(kubriko)
        log.record(this, LifecycleLog.Kind.ADDED)
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        super.update(deltaTimeInMilliseconds)
        log.record(this, LifecycleLog.Kind.UPDATE)
    }

    override fun dispose() {
        super.dispose()
        log.record(this, LifecycleLog.Kind.DISPOSED)
    }

    override fun onRemoved() {
        super.onRemoved()
        log.record(this, LifecycleLog.Kind.REMOVED)
    }

    override fun toString() = "LoggingActor($id)"
}

/** One [Unique] slot. */
class LoggingUniqueA(id: Int, log: LifecycleLog) : LoggingActor(id, log), Unique

/** Another [Unique] slot. */
class LoggingUniqueB(id: Int, log: LifecycleLog) : LoggingActor(id, log), Unique

/** A [Group] that is itself logged. */
class LoggingGroup(id: Int, log: LifecycleLog, override val actors: List<LoggingActor>) : LoggingActor(id, log), Group
