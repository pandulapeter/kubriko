# Second review sweep — follow-ups of the first sweep

**Reviewed commit:** `76992502` on `main` (clean working tree). **Date:** 2026-09-28.
**Angle:** the follow-up candidates the first sweep's lanes reported while landing (`0bdebe2f..76992502`): findings
from its tests-only plans, an `@Ignore`d allocation budget, stale fixture docs and remaining build waste.
**How it was done:** three writer agents (physics, engine, test fixtures and build) re-verified every candidate against
`76992502` — with throwaway probe tests (deleted) and a throwaway worktree for the build change — and wrote the plans.
All seven held.

**Challenge pass (done 2026-09-28).** Two fresh challengers tried to break every fix against in-repo callers, the
documented usage, Tesselar, the other plans and the threading/ordering each fix introduces. Result: **3 sound,
4 amended, 0 dropped.** The amendments that matter:
- `01`: the planned fix kept the old sloped-line branch, which misses ~98 % of hits for rays built from an angle
  (never exactly vertical: explosions, shadow casting, rotating boxes). Replaced by one allocation-free
  perpendicular-distance test; 0 misses and 0 false hits in 20 000 random rays against a float64 reference.
- `02`: bit-for-bit determinism confirmed; the ~710 B/tick estimate holds.
- `05`: the KDoc pointed at the wrong conversion helper; now shows `SceneOffset(pixels / viewportManager.scaleFactor.value)`.
- `07`: its verification grep wrongly expected zero `PackageJson` tasks; the modules' own ones rightly stay.

## Headlines

- **Physics raycasts miss edges** (`01`, high): exact float comparison in the ray–segment test makes most
  axis-aligned rays miss a perpendicular edge (91 % at random offsets), a ray along y=0 miss everything (`-0.0`), and
  a downward ray report the far edge instead of the near one.
- **Physics allocates per contact** (`02`): 56 B per contact per step; a truly resting 200-body scene allocates
  ~12 KB/tick.

## Index

| # | Plan | Severity | Artifact |
|---|---|---|---|
| 01 | Compare raw floats with a tolerance in the physics ray segment test so axis-aligned rays hit polygon edges | high | `plugin-physics` |
| 02 | Stop allocating per contact in the physics step (unbox the contact, recycle arbiters without a copy, optionally reuse the collision result) | medium | `plugin-physics` (Step 3: also `plugin-collision`) |
| 03 | Make `SceneOffset.clamp` and `SceneUnit.clamp` honour a `max` given without a `min` | low | `engine` |
| 04 | Clamp `ViewportManager.newInstance(initialScaleFactor)` to the minimum and maximum scale factors | low | `engine` |
| 05 | Document that `SceneOffset.toOffset` converts a scene-space vector and ignores the camera | low | `engine` |
| 06 | Drop the obsolete zero-sized-viewport sleep workaround from the test fixtures' KDoc and from tests that don't need it | low | none (tests, unpublished fixtures) |
| 07 | Stop `build` from linking iOS and Wasm test binaries (and the repo-wide npm install) for disabled test runs | low | none (build logic) |

## Lanes

One lane, in numeric order, in the main checkout — seven plans do not justify worktrees. Required orderings (all
satisfied by numeric order): `03` before `05` (both edit `SceneOffsetExtensions.kt`, different functions); `02` and
`06` edit different hunks of `PhysicsContractTest.kt`, so `06`'s verification reruns the physics tests, including
`02`'s allocation budget. `01` overlaps no other plan.

## Decisions

| # | Question | Recommended | Answer |
|---|---|---|---|
| 02 | Step 3: add one public overload to `plugin-collision` that reuses a caller-owned `CollisionResult`, removing the last per-contact allocation (a resting scene otherwise still allocates ~6.4 KB/tick)? Additive only; Tesselar uses neither physics nor `collisionResultWith`. | Approve | **Approve** (2026-09-28) |
| 03 | `clamp(max = …)` without `min` returns the value unchanged: fix it to clamp as the KDoc promises, or document that a lone `max` is ignored? No caller in the repo or Tesselar passes a lone `max`. | Fix | **Fix** (2026-09-28) |
| 04 | An out-of-range `initialScaleFactor` is stored unclamped: clamp at construction like every later change, or document it? No caller in the repo or Tesselar is out of range. | Clamp | **Clamp** (2026-09-28) |

## Dropped after verification

None.

## Manual checks owed

- `01`: the physics demo's explosions and any shadow casting still look right; Annoyed Penguins' raycasts.
- `02`: none beyond the budget test (the determinism test pins the simulation).
- `07`: none — CI's `desktopTest` is untouched; the Showcase apps don't use the convention plugin.
