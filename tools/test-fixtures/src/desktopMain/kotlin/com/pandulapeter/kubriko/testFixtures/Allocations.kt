/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testFixtures

import org.junit.Assume
import java.lang.management.ManagementFactory

/**
 * Returns the average number of bytes the calling thread allocated per run of [block], measured over [measuredRuns]
 * runs after [warmUpRuns] runs that give the JIT time to compile [block] (interpreted code allocates boxes that
 * compiled code does not). Skips the test if the JVM cannot measure per-thread allocation.
 *
 * The result is an upper-bound check that only makes sense for work done on the calling thread.
 */
fun measureAllocatedBytesPerRun(
    warmUpRuns: Int = 20_000,
    measuredRuns: Int = 2_000,
    block: () -> Unit,
): Double {
    val threadMXBean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
    Assume.assumeTrue(threadMXBean.isThreadAllocatedMemorySupported)
    threadMXBean.isThreadAllocatedMemoryEnabled = true
    repeat(warmUpRuns) { block() }
    val threadId = Thread.currentThread().threadId()
    val allocatedBytesBefore = threadMXBean.getThreadAllocatedBytes(threadId)
    repeat(measuredRuns) { block() }
    val allocatedBytesAfter = threadMXBean.getThreadAllocatedBytes(threadId)
    return (allocatedBytesAfter - allocatedBytesBefore).toDouble() / measuredRuns
}
