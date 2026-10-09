# Trim the fixed-bug history from `PhysicsManagerImpl`'s comments and turn its declaration comments into KDoc

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-physics
**Files:**
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/PhysicsManagerImpl.kt`
- `plugins/physics/CLAUDE.md`

## Problem
The code-style skill: never explain a bug that is already fixed (a regression guard keeps only the constraint), and a
comment documenting a declaration is KDoc, not `//`. At 2480325f `PhysicsManagerImpl.kt` has:
- `//` on declarations: `accumulatedTimeInMilliseconds` (:64), `step` (:120–122), `broadPhaseCheck` (:223–227), and
  the three companion constants (:316–319, :322–324, :327–328).
- History: :71–80 (a ten-line essay on why a whole-tick Euler step was unstable); :216–219 "body.force *now* persists …
  Applying it here is equivalent to the *previous* `applyForce(drag)` followed by …"; :226 "the exact (i, j) order *the
  previous nested-loop implementation* produced"; :316–319 "≈ *the previous variable-delta step*, so the engine behaves
  the same … and only changes — for the better — when ticks are throttled".

`plugins/physics/CLAUDE.md` repeats history: :81–82 "so typical-frame-rate behavior is essentially unchanged; the
accumulator only changes things when throttled", :87 "(this was the bug behind the frame-rate-dependent penguin
launch)", :90 "Because `force` *now* persists", :95 "that relied on the old whole-tick integration and breaks under
sub-stepping".

## Fix
Comments only. Keep every regression guard as one or two lines stating the constraint; drop the history. Suggested
wording (match the file's voice; exact phrasing is the executor's):
- `accumulatedTimeInMilliseconds`: `/** Real time carried between ticks by the fixed-timestep accumulator in [onUpdate]. */`
- :71–80 → `// Fixed-timestep accumulator: the tick's real time is split into constant FIXED_TIME_STEP_IN_MILLISECONDS
  sub-steps, each advancing simulationSpeed times that quantum, so integration stays stable at any frame rate.`
- :91–96 → keep as two lines: forces apply across every sub-step and are cleared once per tick, only when a step ran,
  so a force set on a tick that ran no step survives to the one that does.
- :100–102 spiral-of-death guard: keep (it is a constraint, not history).
- `step` (:120–122) → KDoc on `step`. The sync comment inside it (:128–135) → two lines: integration moves the body,
  not its mask, so the mask is refreshed before every broad phase; do not hoist it out of `step`.
- :168–169 (force not cleared per sub-step): keep.
- :216–219 → `// Integrated straight into velocity: body.force persists across the tick's sub-steps, so drag routed
  through it would accumulate.`
- `broadPhaseCheck` (:223–227) → KDoc: sweep-and-prune over bodies sorted by AABB left edge; candidate pairs are
  re-sorted into the `(i, j)` order of a nested loop, which keeps the solver's arbiter order and so the results
  deterministic — do not remove the pair sort.
- Companion constants → KDoc each. `FIXED_TIME_STEP_IN_MILLISECONDS`: "16 ms × the default simulationSpeed 1 / 100 is
  the per-step dt the simulation was tuned against at 60 FPS." Keep the other two's content (they state constraints).

`plugins/physics/CLAUDE.md`: delete the parenthetical at :87; "now persists" → "persists" (:90); :81–82 → "16 ms is the
per-step dt the engine was tuned against at 60 FPS (`16 * simulationSpeed 1 / 100`)."; :95 → "it breaks under
sub-stepping". Leave the "Do not remove this sync" / "Do not optimize away the pair sort" guards.

## Behaviour
Unchanged — comments and Markdown only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:physics:compileKotlinDesktop`

## Manual check
none
