# Turn `ParticleManagerImpl`'s declaration comments into KDoc and drop the history phrase from `ParticleBatch`

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-particles
**Files:**
- `plugins/particles/src/commonMain/kotlin/com/pandulapeter/kubriko/particles/ParticleManagerImpl.kt`
- `plugins/particles/src/commonMain/kotlin/com/pandulapeter/kubriko/particles/implementation/ParticleBatch.kt`

## Problem
The code-style skill: a comment documenting a declaration is KDoc, never `//`; no fixed-bug history. At 2480325f:
- `//` on declarations in ParticleManagerImpl.kt: `EmissionAccumulator` (:31–32), `batches` (:49–51), `recycle`
  (:54).
- `ParticleBatch` KDoc (ParticleBatch.kt:23–26): "so spawning and recycling **no longer** add and remove an actor per
  particle".

## Fix
- Turn the three `//` blocks into KDoc on the same declarations, same content.
- `ParticleBatch` KDoc: "Draws every live particle that shares one [drawingOrder] in a single actor, so spawning and
  recycling never add or remove an actor per particle. …" (rest unchanged).
- Leave the `//` notes inside `onUpdate` (statements, not declarations).

## Behaviour
Unchanged — comments only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:particles:compileKotlinDesktop`

## Manual check
none
