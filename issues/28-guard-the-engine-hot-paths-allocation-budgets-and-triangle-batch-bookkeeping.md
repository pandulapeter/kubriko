# Guard the engine hot paths' allocation budgets and TriangleBatch's index bookkeeping with tests

**Challenged:** amended — the moving actors must stay clear of the viewport and far-away-sleep edges, because a changed visible or active set legitimately republishes an O(n) list and would turn the per-actor budget into a flaky figure; the `engine/CLAUDE.md` sentence goes under *Culling allocation model* (that file has no performance section); the executor's timing expectations are stated. A probe at `e86d3748` checked the budgets: 88–89 B per tick for 100 and for 10 000 actors (half asleep), 107–112 B with oscillating `Visible` actors, and 0 B for the `BoxBody` refresh, the `Timer` and 1 000 quads plus 200 anti-aliased lines. The per-actor work (update loop, both culls, the mirror refill) runs on the calling thread, so the thread-local measurement covers it. After plan `06` no derived list is rebuilt on another thread.

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all (measured on the desktop JVM)  ·  **Artifact:** `engine` (tests only)
**Files:** `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/performance/HotPathAllocationTest.kt` (new), `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/helpers/TriangleBatchBookkeepingTest.kt` (new), `engine/CLAUDE.md` (one sentence under *Culling allocation model*)

**Part of the testing extension.** It runs in lane A after every lane-A fix (`01`–`25`), because `06` and `09`
change the tick loop that this plan measures. It adds tests only.

## Problem

`CLAUDE.md` makes zero per-frame allocation a hard rule ("Performance is of paramount importance"), and three
optimisation passes have enforced it by reading code and profiling by hand. Nothing checks it automatically. A
capturing lambda or a boxed `List<Float>` added to `onUpdate` compiles, passes every functional test and costs
frames only on low-end Android and Wasm. The live stress run measured the steady state at about 100 B per tick
regardless of actor count, and the fixtures probe at `e86d3748` measured 72 B per tick with one actor. This plan
turns those measurements into budgets. The lane-A fixes themselves (`03`, `06`) touch exactly this loop.

`TriangleBatch` promises that "steady-state filling does not allocate" and that a batch holds at most
`MAX_INDEXED_VERTICES` unique vertices. The only enforcement is a hard `check` in `ensureVertexCapacity`.
`TriangleBatchTexCoordTest` (`21`) covers texture coordinates only.

## Fix

**`HotPathAllocationTest`** (desktopTest). It measures with `measureAllocatedBytesPerRun` from
`:tools:test-fixtures`, which uses the calling thread's allocation counter. `ManualTickSource.tick` runs
`KubrikoImpl.onTick` synchronously on the caller (`emitTick` → `kubrikoImpl.onTick`), so the whole tick is
measured. Each test uses `newTestKubriko` (plan `01`, sized viewport) and first calls `awaitProcessed()` so no batch
is in flight.
- `tickCostIsFlatInActorCount` — for `n` in `100` and `10_000`, add `n` `Dynamic` actors whose `update` does
  non-allocating work (increment an `Int` field). Half are `Positionable` inside the viewport and half outside, with
  `shouldPutFarAwayActorsToSleep = true`, so the sleep partition runs. Measure `tick(16)` per run.
  Assert that each measurement is at most **1 024 B per tick**, and that the 10 000-actor figure is within
  **256 B** of the 100-actor figure. The count independence is the contract; the absolute ceiling is only headroom
  over today's ~100 B.
- `tickWithVisibleMovingActorsDoesNotAllocatePerActor` — as above with `Visible` `BoxBody` actors (10×10, a no-op
  `draw()`) whose `update` moves `body.position` back and forth by one unit each tick and reads
  `body.axisAlignedBoundingBox`, with the same budgets. Place the inside half near the camera and the outside half
  around `(100 000, 0)`, so that no actor ever crosses the viewport edge or the far-away-sleep margin. When the
  visible or active set changes, the engine republishes an immutable list of the whole set. That allocation is
  proportional to `n`, and it is correct behaviour, so a drifting actor would fail the count-independence budget
  for the wrong reason. Keep `drawingOrder` constant for the same reason (the draw-cache reuse check). Nothing
  draws: drawing happens in composition, not in the tick.
- `boxBodyRefreshDoesNotAllocate` — alternately set `position` and `rotation` on one `BoxBody` and read
  `axisAlignedBoundingBox`: **0 B** per run, allowing `< 1` for measurement noise.
- `timerUpdateDoesNotAllocate` — a repeating `Timer` whose `onDone` increments a field: `< 1` B per `update(16)`.
- `triangleBatchSteadyStateDoesNotAllocate` — per run, `reset()`, then 1 000 `addQuad(...)` colored quads plus 200
  `addLine(...)` calls with anti-aliasing on. After the warm-up has grown the buffers: `< 1` B per run. `flush` is
  not called, because it needs a Skia canvas.

Budgets are upper bounds, so JIT escape analysis can only make them pass more easily. With the default warm-up
(20 000 runs), the 10 000-actor measurements take roughly 0.3–0.7 s each on an Apple Silicon desktop JVM, and the
whole class takes about 2 s. If a figure is flaky on the
executor's machine (varies by more than 2× across three runs), raise the warm-up; never raise the budget without
recording why in the test.

**`TriangleBatchBookkeepingTest`** (commonTest, public API only):
- `addVertex` returns `0, 1, 2, …` and restarts at `0` after `reset()`. `reset()` increments `generation` and makes
  `isEmpty` true.
- `willOverflow(k)` is false while `count + k <= MAX_INDEXED_VERTICES` and true once it would exceed it. Check at
  exactly the boundary.
- Adding past the limit without a reset fails with `IllegalStateException` (the `check`). After `reset()` the batch
  accepts vertices again.
- Shared corners: two triangles built from four `addVertex` indices leave the vertex count at 4, so
  `willOverflow(MAX_INDEXED_VERTICES - 4)` is false and `willOverflow(MAX_INDEXED_VERTICES - 3)` is true.
- `addLine` shorter than `0.001` adds nothing and leaves `isEmpty` true (the `if (length < 0.001f) return` guard).
  A NaN endpoint must not throw; assert only that it does not.

**`engine/CLAUDE.md`**: one sentence at the end of *Draw-Cache Invalidation* → *Culling allocation model* saying that `HotPathAllocationTest` holds the tick
loop, `BoxBody` refresh, `Timer` and `TriangleBatch` to their allocation budgets, and that a change which trips it
must fix the allocation, not the budget.

## Tests

This plan is the tests. Run `./gradlew :engine:desktopTest` three times; every run must pass. Then run
`./gradlew :engine:build` for the commonTest compile.

## Manual check

None.
