# Check the physics simulation's invariants and its determinism for identical inputs

**Challenged:** amended — bodies start apart (the body list is published on `Dispatchers.Default` and can switch from empty to full between two phases of one step, which is harmless only while nothing touches); `noGravityNoMotion` gets a moving probe body, since it has nothing else to detect registration by; the resting-ball test uses `restitution = 0f` and realistic tolerances (a ball dropped 200 units falls ~10 units per 16 ms step at impact, so a 2-unit penetration bound would fail on the impact tick); the head-on circles are big enough not to pass through each other in one step; `simulationSpeedZero` freezes before any contact (a zero-length step still runs penetration correction); the allocation budget is asserted on a contact-free scene, while the resting scene is only measured and reported, because every contact allocates a `CollisionResult` at HEAD.

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all (runs on the desktop JVM)  ·  **Artifact:** `plugin-physics` (tests only)
**Files:** `plugins/physics/src/desktopTest/kotlin/com/pandulapeter/kubriko/physics/PhysicsContractTest.kt` (new)

**Part of the testing extension.** It is the last plan of lane C, after `49`–`53`, which change the accumulator, the
body lists, the sweep order, explosions and raycasts. It adds tests only.

## Problem

The physics plugin is a port of JPhysics, tuned for performance: pooled arbiters, sweep-and-prune with index
re-sorting, raw-float maths. Plans `49`–`53` each test one bug. Nothing tests that the simulation still behaves like
physics (resting bodies rest, static bodies stay, collisions conserve momentum), or that it is deterministic.
Determinism is what makes a replay, a networked game or a regression test possible at all, and nothing breaks it
loudly. The research for this plan found no `Random`, no hash-ordered collection and no clock in `physics/`, and the
sweep re-sorts pairs into index order (`PhysicsManagerImpl.kt`, "sort pairs back into (i, j) order"). So the
simulation should be deterministic for identical inputs today. A future optimisation that iterates a `HashSet` would
silently end that.

## Fix

Add `PhysicsContractTest` in `desktopTest`. Every test builds its instance with
`newManualKubriko(ActorManager.newInstance(shouldComposeLayers = false, shouldPutFarAwayActorsToSleep = false), PhysicsManager.newInstance(...))`
from `:tools:test-fixtures`. It adds all bodies in **one** `add(list)` call, so they register in the same tick, and
waits for registration with `tickUntil { any dynamic body moved }`. The first-tick lag of `50` does not matter after
that. Keep one small private `RigidBody` actor class in the file, built from a `PhysicsBody` around a
`CircleCollisionMask` or `BoxCollisionMask`. Static bodies are `density = 0f`; their masks are never re-synced from the
body (`syncCollisionMasksWithBodies` skips `invMass == 0`), so build a static box's mask at its final position and
rotation (`initialRotation`) and give the `PhysicsBody` the same rotation.

Every scene starts with **no two bodies touching** (bounding boxes at least a few units apart, bodies above the floor
and ramps). The body list reaches `PhysicsManagerImpl` through a `stateIn` on `Dispatchers.Default` and is read
several times per step, so on the registration tick the list can switch from empty to full between the broad phase
and the integration. With nothing touching that cannot change the outcome; with overlapping bodies one run could
skip a contact the other resolves, which would look like a determinism failure. In `identicalRunsAreBitForBitIdentical`,
place the 60 bodies on a grid with `Random(42)` jitter smaller than the spacing, and use a fresh `Random(42)` per run.
- `identicalRunsAreBitForBitIdentical` — build two instances with the same scene: 60 circles and boxes, positioned
  by `Random(42)`, falling onto a static floor and two static ramps. Register both, then run exactly 600 ticks of
  16 ms each. After every 100 ticks, every body's `position`, `rotation` and `velocity` must be equal across the two
  runs, component by component, with `toRawBits`. Run the two instances one after the other, not interleaved. The
  collision plugin's narrow-phase scratch buffers are file-level globals; every one of them is written before it is
  read within a call, so sequential runs share no state through them.
- `staticBodiesNeverMove` — in the scene above, the floor's and ramps' positions and rotations are unchanged after
  600 ticks (bitwise).
- `aBallComesToRestOnTheFloor` — one circle of radius 10, `restitution = 0f` (the contact uses the smaller of the
  two restitutions, so the ball does not bounce), dropped from 200 units above a static box floor at least 50 units
  thick, with default gravity. Run 900 ticks. With 16 ms steps (`dt = 0.16` simulation units) the ball reaches about
  63 units per simulation unit at impact, so it moves ~10 units per step and can end the impact step up to ~10 units
  inside the floor before the contact is resolved. Assert:
  - It never tunnels: at every tick its center (`y`) stays above `floorTop` (penetration below the radius).
  - It ends at rest: over the last 60 ticks its `y` varies by at most 1 unit and the mean `|velocity|` is below
    `0.5`, and its final `y + 10` is within 2 units of `floorTop` (penetration correction removes 20 % of the overlap
    per step, so the overlap decays to well under a unit).
- `noGravityNoMotion` — with `initialGravity = SceneOffset.Zero`, a body with zero velocity keeps its position
  exactly for 300 ticks. It cannot signal registration by moving, so add a second body far away (1 000 units) with
  velocity `(10, 0)` and wait until that one moves.
- `headOnCollisionConservesMomentum` — zero gravity, two equal circles of radius 20 (same radius and density, so equal
  mass; `mass` is internal), `restitution = 1f` on both, all frictions `0`, starting 200 units apart and moving
  towards each other at `(+50, 0)` and `(-50, 0)` on the same horizontal line (they close 16 units per step, less
  than the 40-unit sum of radii, so the contact is always seen). Before and after contact:
  - `|v1.x + v2.x| <= 0.5` at every tick (1 % of one body's initial speed).
  - Their y velocities stay `0` within `1e-3`.
  - After contact they separate. They move apart, and neither passes through the other.
- `simulationSpeedZeroFreezesTheWorld` — falling bodies that are still apart; set `simulationSpeed.value = 0f` right
  after registration. Positions are unchanged over 100 ticks. (A zero-speed step still runs the broad phase and
  penetration correction, so bodies already in contact would be pushed apart; freezing before any contact is what
  the test pins.)
- `pausedStateStopsTheSimulation` — `kubriko.get<StateManager>().updateIsRunning(false)`; `isRunning.value` is then
  false at once (it is a synchronous flow over the focus and running flags), and `awaitCondition { !isRunning.value }`
  merely confirms it. Positions are unchanged over 100 ticks, and they move again after `updateIsRunning(true)`.
- `steadyStateTickAllocationIsBounded` — two measurements, each one 16 ms tick per run with
  `measureAllocatedBytesPerRun(warmUpRuns = 2_000, measuredRuns = 500) { tick() }`:
  - 200 bodies drifting apart with zero gravity and no contacts. Use the same rule as `62`: assert a 1 024 B per tick
    budget if the first measurement is within it; otherwise `@Ignore` with the measured number and report it.
  - 200 bodies resting on a floor: measure and report the number only, in a separate test marked
    `@Ignore("measured <n> B/tick with 200 contacts")`. At HEAD every contact allocates: `Arbiter.narrowPhaseCheck`
    calls `collisionResultWith`, which builds a `CollisionResult`, and writes `contacts[0]` into an
    `Array<SceneOffset>`, which boxes. That is a performance follow-up, not part of this plan.

## Tests

This plan is the tests. Run `./gradlew :plugins:physics:desktopTest` twice, then `./gradlew :plugins:physics:build`.
A determinism failure is a real bug. Report which body diverged first, at which tick; do not loosen the comparison.
The rest and tunnelling tolerances may be widened at most 2×, with the reason in a comment. Beyond that, `@Ignore`
the test and report it.

## Manual check

None.
