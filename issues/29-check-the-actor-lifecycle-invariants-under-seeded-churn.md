# Check the actor lifecycle invariants under a seeded, randomized churn of actor operations

**Challenged:** amended — five points were not guaranteed after plans `01`–`09` and would have failed on correct code:
- **Invariant 3** demanded an unmatched `ADDED` before every `UPDATE` in sequence order. Plan `06` knowingly leaves a batch that lands during a running tick free to call `onRemoved()` before that tick's `update()` of the same actor. The rule is now by tick index only.
- **Invariant 4** was checked on a tick that already carried the next random operations. It now runs on a dedicated tick with no operations.
- **Membership** counted the fixtures' `Blocker`s, which stay in the scene, against the model.
- **The cross-thread dispose test** asserted invariant 5 in full. Plans `08`/`09` guarantee neither the exact `DISPOSED` set nor "no event after `dispose()` returns" when a batch or a tick is in flight on another thread, and its `awaitProcessed` would have timed out once the processor was cancelled. It now asserts only what those plans guarantee.
- **The model and generator** now state the flatten-first and presence-before-eviction rules. They keep `Unique`s and `removeAll` out of the concurrent test, where they cross the per-thread partitions.

**Kind:** test  ·  **Severity:** high  ·  **Platforms:** all (runs on the desktop JVM)  ·  **Artifact:** `engine` (tests only)
**Files:** `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/lifecycle/LifecycleLog.kt` (new), `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/lifecycle/ActorLifecycleChurnTest.kt` (new), `engine/CLAUDE.md` (Actor Batch Processing: one sentence), root `CLAUDE.md` (Testing paragraph: one sentence on `KUBRIKO_STRESS`)

**Part of the testing extension.** This is the last plan of lane A, because it asserts the contract that `01`–`09`
establish together. It adds tests only.

## Problem

The review's headline ActorManager bugs (`01`–`07`) were found by a one-off churn run in the scratchpad: 5 000 ticks
× 20 adds + 20 removes produced 506 duplicated actors and 1 432 callback mismatches. The fix plans each add one
test for one scenario. Nothing re-runs the churn. So a later change to `ActorManagerImpl.processBatch` (the most
performance-tuned code in the engine) can break pairing, ordering or membership in a combination that no
single-scenario test covers. `CLAUDE.md` documents the contract (`onAdded` "right before addition", `onRemoved`
"right after removal", `dispose()` "before `onRemoved()`", `Unique` replacement, `Group` flattening), but no test
checks it as a whole.

## Fix

**`LifecycleLog`** — a test helper, thread-safe.
- `class LoggingActor(val id: Int) : CountingActor()` records events into a shared log, and overrides the
  fixtures' `CountingActor` callbacks while calling `super`. Each event is `(sequence: Long from a shared
  AtomicLong, actorId, kind: ADDED | UPDATE | DISPOSED | REMOVED, tick: Int, thread)`.
  - It is not `Positionable`, so it is always active, whatever the viewport size (`ActorManagerImpl` puts only
    `Positionable` actors to sleep).
  - `LoggingUnique : LoggingActor, Unique` has one class per "slot" (two classes are enough).
  - `LoggingGroup : LoggingActor, Group` has a fixed `actors` list.
- `tick` in each event is a shared `@Volatile var currentTick` that the driver increments *before* each `tick(16)`.
- `fun checkInvariants(log, expectedMembers: Set<Int>, liveMembers: List<Actor>)` checks every invariant below and
  fails with the seed, the actor id and that actor's event sequence. It looks only at `LoggingActor`s. The fixtures'
  sentinel and `Blocker`s are plain actors with no id. A `Blocker` stays in the scene after its release (until a
  `removeAll`), so both are filtered out of `allActors.value` before comparing.

**Invariants** (checked at every quiescent point, i.e. after `awaitProcessed()`, and at the end). When
`awaitProcessed()` returns, every callback of every earlier operation has returned too (see its KDoc in plan `00`),
so the log is complete up to that point:
1. **Pairing:** each actor's non-`UPDATE` events recorded before `kubriko.dispose()` alternate `ADDED`,
   (`DISPOSED`), `REMOVED`, `ADDED`, …. There is never a `REMOVED` without a preceding unmatched `ADDED`, never two
   `ADDED` in a row, and `DISPOSED` comes immediately before its `REMOVED` (`04`, `01`). The `DISPOSED` that
   `kubriko.dispose()` adds without a `REMOVED` is invariant 5's business.
2. **Membership:** `allActors.value` has no duplicates by identity (`03`). Its `LoggingActor` ids equal
   `expectedMembers` from the sequential model below. A `LoggingActor` is present iff its `ADDED` count exceeds its
   `REMOVED` count by exactly 1.
3. **No update outside membership, by tick index:** every `UPDATE` of actor `x` with tick `u` has an `ADDED` of `x`
   earlier in sequence order, and no `REMOVED` of `x` between that `ADDED` and the `UPDATE` (in sequence order) has
   a tick smaller than `u` (`06`).
   - An `UPDATE` recorded *after* a `REMOVED` of the same tick is allowed. Plan `06` publishes the derived lists
     before `onRemoved()` and refreshes the active set before the update loop, but a batch applied while a tick is
     already iterating can still call `onRemoved()` before that tick reaches the actor. Plan `06` documents this.
   - An `UPDATE` in a *later* tick is a bug. `onRemoved()` reads `currentTick` after the removal was published, and
     the driver writes `currentTick` before calling `tick`. So if `REMOVED` recorded tick `t`, the publication came
     before the next tick's refresh.
   - The `ADDED`-before-`UPDATE` half holds strictly: `onAdded` runs before its batch is published.
4. **Every member is updated once:** right after the `awaitProcessed()` of a check, the driver runs one extra
   `currentTick++; tick(16)` **without issuing any operation**. In that tick every member records exactly one
   `UPDATE` and no non-member records any (`05`–`07`, sized viewport, and the pool actors are not `Positionable`).
   Do not use the next churn tick for this: its operations are issued before it and can be applied before its
   refresh, which legitimately changes who is updated.
5. **Dispose (single-threaded driver only):** the driver calls `awaitProcessed()` right before `kubriko.dispose()`.
   Afterwards every model member has exactly one extra `DISPOSED` and no extra `REMOVED` (`08`), and nothing else
   gets a `DISPOSED`. No event of any kind is recorded after `dispose()` returns, even though the driver keeps calling
   `tick(16)` (the tick source is stopped, `09`).

**Sequential model:** apply each operation, in the order the test issued it, to a `LinkedHashSet<Int>`:
- Flatten first. An operation on a `Group` is the same operation on the group and on each of its children, applied
  member by member. So removing an absent group still removes its present children, and adding a present group
  still adds its absent children (plan `03` filters per actor, after flattening).
- `add` of a present actor is a no-op. Check this before the `Unique` rule: re-adding the live `Unique` instance
  evicts nothing (`03`).
- Adding an absent `Unique` removes the present instance of the same class.
- `remove` of an absent actor is a no-op.
- `removeAll` clears the set.

This mirrors the documented semantics (`CLAUDE.md` → ActorManager, `Group`, `Unique` KDoc, decisions `03`/`04`),
not the implementation's batching. Membership after a quiescent point must equal the model, however the operations
were batched.

**`ActorLifecycleChurnTest`** — each test takes its seeds and sizes from a private `ChurnConfig`, where
`val isStress = System.getenv("KUBRIKO_STRESS") == "1"`.
- `singleThreadedChurnKeepsTheInvariants` — for each seed, build a fresh
  `newTestKubriko(actorManager = ActorManager.newInstance(shouldComposeLayers = false))` with a pool of `LoggingActor`s,
  two `LoggingUnique` classes with three instances each, and a few `LoggingGroup`s with disjoint children drawn from
  the pool.
  - Each tick issues 0–5 random operations from `Random(seed)`: `add` of 1–4 random plain pool actors (present
    ones included, on purpose), `remove` likewise, `add`/`remove` of a group, `add` of one unique instance per call,
    and `removeAll` with 1 % probability.
  - Then `currentTick++; tick(16)`. Every 25 ticks, `awaitProcessed()`, check invariants 1–3, then run invariant 4's
    operation-free tick.
  - At the end, `awaitProcessed()`, `dispose()`, tick three more times, and check invariant 5.
  - Defaults: seeds `1..3`, 400 ticks, pool 200. Stress: seeds `1..50`, 5 000 ticks, pool 2 000.
- `blockedBatchesKeepTheInvariants` — the same driver, but every 10th tick it adds a fixtures `Blocker`, issues that
  tick's operations while the processor is held, and then releases. This forces add-then-remove,
  remove-then-add and unique replacement into one batch (`04`). The model does not include the blocker. Defaults:
  seeds `1..3`; stress: `1..20`.
- `concurrentCallersKeepTheInvariants` — **stress only**, skipped with `Assume` otherwise. Four threads each own a
  disjoint quarter of the pool (so per-thread caller ordering, which the review found solid, makes each quarter's
  model exact) and issue random `add`/`remove` operations while the test thread ticks.
  - A group used here has all its children in its thread's quarter.
  - No `Unique` and no `removeAll`: a unique eviction or a `removeAll` issued by one thread changes another thread's
    actors, whose order relative to it is not defined, so no per-quarter model could predict the result.
  - Join the threads, `awaitProcessed()`, check invariants 1–3 against the union of the four models, then run
    invariant 4's operation-free tick.
- `disposeFromAnotherThreadDuringChurn` — **stress only.** While churning, a second thread calls `dispose()` at a
  random tick (`09`/`10`), and the driver keeps ticking.
  - The driver takes the `ActorManager` and tick source references before it starts, because `kubriko.get()` throws
    after `dispose()`.
  - Once the dispose thread has been started, the driver stops calling `awaitProcessed()` and the checks. After plan
    `08` a disposed instance processes nothing, so they would time out.
  - A batch or a tick that is in flight on another thread when `dispose()` runs is not stopped by plans `08`/`09`.
    The batch may still call `onAdded`, and its removal callbacks may be skipped. The tick may still update actors
    that `dispose()` has already disposed. So invariant 5's exact form does not apply. Assert what is guaranteed:
    - no exception escapes `tick()` or reaches `recordingUncaughtExceptions`;
    - invariant 1 holds, allowing one trailing `DISPOSED` per actor, and no actor has two `DISPOSED` without an
      `ADDED` between them;
    - once the dispose thread has been joined, let `t0 = currentTick`. The driver ticks three more times, and no
      `UPDATE` has a tick greater than `t0` (the tick source is stopped once `dispose()` has returned).

Failure messages start with `seed=<n> tick=<t>` so that a nightly failure reproduces locally with the same seed.
Wall time with the defaults must stay under 5 s on the executor's machine; if it does not, shrink the defaults, not
the invariants.

**Docs:**
- `engine/CLAUDE.md` → Actor Batch Processing: `ActorLifecycleChurnTest` checks the callback-pairing and membership
  contract under randomized churn, and any change to `processBatch` must keep it green with `KUBRIKO_STRESS=1`.
- Root `CLAUDE.md` → Testing: `KUBRIKO_STRESS=1 ./gradlew :engine:desktopTest` runs the long randomized suites (the
  nightly CI job, plan `89`, does this).

## Tests

This plan is the tests. Run `./gradlew :engine:desktopTest` three times, then
`KUBRIKO_STRESS=1 ./gradlew :engine:desktopTest --tests "*ActorLifecycleChurnTest*"` once. All must pass. A
violation means a lane-A fix is incomplete. Report it with the seed; do not weaken the invariant. If the cause is
clear and inside `ActorManagerImpl`, stop the lane at this plan and tell the orchestrator.

## Manual check

None.
