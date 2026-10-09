# Move `CollisionTestStateHolderImpl` out of `CollisionTestStateHolder.kt` into `CollisionTestStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-collision-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/CollisionTestStateHolder.kt`, `examples/test-collision-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/CollisionTestStateHolderImpl.kt` (new), `examples/test-collision-noop/CLAUDE.md`

## Problem
`examples/test-collision-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/CollisionTestStateHolder.kt` holds `sealed interface CollisionTestStateHolder : StateHolder` (:17, with its `companion object` resource gate) and `internal class CollisionTestStateHolderImpl` (:25–30). One top-level type per file, named after it.

## Fix
- Create `examples/test-collision-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/CollisionTestStateHolderImpl.kt` (MPL-2.0 header copied from the old file, same package) and move the whole `internal class CollisionTestStateHolderImpl` verbatim.
- The old file keeps only the interface and its companion; split the imports so each file keeps exactly what it uses.
- A sealed interface may be implemented in another file of the same package and module; callers import the Impl by package, which does not change.
- In `examples/test-collision-noop/CLAUDE.md`, "Two files that mirror…" becomes "Three files…" and the `CollisionTestStateHolder.kt` bullet is split into the interface bullet and a `CollisionTestStateHolderImpl.kt` bullet.
- Grep the repo (CLAUDE.md files, skills, docs) for `CollisionTestStateHolder.kt` and fix every path reference in the same commit.

## Behaviour
Verbatim move within one package; nothing else changes.

## Public API
None (examples are unpublished). The noop must keep mirroring `test-collision`'s file layout and API after D31.

## Tests
The existing ones (the module has none).

## Verify
`./gradlew :examples:test-collision-noop:compileKotlinDesktop` (the Showcase links the noop only with `showcase.areTestExamplesEnabled=false`)

## Manual check
none
