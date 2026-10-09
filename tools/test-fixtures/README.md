<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Test Fixtures

Shared helpers for this repository's own unit tests. The module is **not published** and holds only JVM code: every
module's `desktopTest` source set depends on it automatically (wired in `gradle/build-logic`), so tests use these
helpers instead of hand-rolling a harness. It is built on the engine's public API only.

## Helpers

- `newManualKubriko` / `ManualKubriko` - A `Kubriko` instance driven by `TickSource.manual()`, with `tick` (delta first, then count) and `tickUntil` for asynchronous work. It is `AutoCloseable`: `newManualKubriko().use { ... }` disposes it even when an assertion fails.
- `ActorManager.awaitProcessed` - Blocks until every earlier actor operation has been applied, published to `allActors` and had its callbacks run.
- `awaitCondition` - Polls a condition with a timeout; the fallback when no deterministic signal exists.
- `CountingActor` - Counts its `onAdded`, `onRemoved`, `dispose` and `update` calls, records the thread each callback last ran on, and can run an action in each.
- `Blocker` - Holds the actor processor inside `onAdded` so several operations land in one batch.
- `recordingUncaughtExceptions` - Records uncaught exceptions while a block runs.
- `measureAllocatedBytesPerRun` - Measures the calling thread's heap allocation per run of a block, after a JIT warm-up.
