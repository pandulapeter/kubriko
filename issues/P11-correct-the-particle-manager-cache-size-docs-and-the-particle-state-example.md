# Correct the `ParticleManager` `cacheSize` KDoc and the particles CLAUDE.md example, and document `reuseParticleInternal`

**Kind:** docs  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-particles
**Files:**
- `plugins/particles/src/commonMain/kotlin/com/pandulapeter/kubriko/particles/ParticleManager.kt`
- `plugins/particles/src/commonMain/kotlin/com/pandulapeter/kubriko/particles/ParticleEmitter.kt`
- `plugins/particles/CLAUDE.md`

## Problem
At 2480325f:
- `ParticleManager.newInstance` KDoc (ParticleManager.kt:32): "`@param cacheSize` The maximum number of particles that
  can be active at once." It is actually the cap on recycled states kept **per state type**
  (ParticleManagerImpl.kt:55–60: `val deque = cache.getOrPut(state::class) { ArrayDeque() }; if (deque.size < cacheSize)
  { deque.addLast(state) }`); the number of live particles is unbounded. `plugins/particles/CLAUDE.md`:30 already says
  "per state type, not global".
- `fun reuseParticleInternal(state: ParticleState) = reuseParticleState(state as S)` (ParticleEmitter.kt:54) is
  public with no KDoc.
- `plugins/particles/CLAUDE.md` "Implementing a `ParticleState`" (:44–55) overrides `reuseParticleState()` and
  `createParticleState()` on the state class — those are `ParticleEmitter` methods, `reuseParticleState` takes
  `state: S`, and its `update` returns `Unit` where the real one returns `Boolean`. The Gotchas (:61) repeat
  `reuseParticleState()` without its parameter.

## Fix
- `cacheSize` KDoc: the maximum number of finished particle states kept for reuse per `ParticleState` type; the number
  of live particles is not limited by it.
- `reuseParticleInternal` KDoc: the untyped entry point the `ParticleManager` calls with a pooled state of
  [particleStateType]; it casts and forwards to [reuseParticleState]. Implementations override `reuseParticleState`,
  not this.
- Replace the CLAUDE.md example with a state class (`body`, `update(...): Boolean`, `draw`) and an emitter that sets
  `particleStateType = MyParticleState::class` and overrides `createParticleState()` and
  `reuseParticleState(state: MyParticleState)` (resetting every mutable field of `state`). Fix :61 to
  `reuseParticleState(state)`. Check the example against `examples/demo-particles` before writing it.

## Behaviour
Unchanged — documentation only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:particles:compileKotlinDesktop`

## Manual check
none
