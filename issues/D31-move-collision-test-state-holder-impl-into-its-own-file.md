# Move `CollisionTestStateHolderImpl` out of `CollisionTestStateHolder.kt` into `CollisionTestStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/CollisionTestStateHolder.kt`, `examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/CollisionTestStateHolderImpl.kt` (new), `examples/test-collision/CLAUDE.md`
**Challenged:** amended — fixed the mangled Gradle task in Verify (`:examples:test-collision:compileKotlinDesktop`).

## Problem
`examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/CollisionTestStateHolder.kt` holds `sealed interface CollisionTestStateHolder : StateHolder` (:26, with its `companion object` resource gate) and `internal class CollisionTestStateHolderImpl` (:37–70) plus `private const val LOG_TAG = "Collision"` (:72). One top-level type per file, named after it.

## Fix
- Create `examples/test-collision/src/commonMain/kotlin/com/pandulapeter/kubriko/testCollision/implementation/CollisionTestStateHolderImpl.kt` (MPL-2.0 header copied from the old file, same package) and move the whole `internal class CollisionTestStateHolderImpl` verbatim together with the file-private `LOG_TAG` (stays `private`; only the Impl uses it).
- The old file keeps only the interface and its companion; split the imports so each file keeps exactly what it uses.
- A sealed interface may be implemented in another file of the same package and module; callers import the Impl by package, which does not change.
- In `examples/test-collision/CLAUDE.md`'s "Module structure" tree, split the `CollisionTestStateHolder.kt — sealed interface + Impl; creates …` entry into `CollisionTestStateHolder.kt` (sealed interface + resource gate) and `CollisionTestStateHolderImpl.kt` (creates CollisionManager, PointerInputManager, ViewportManager, CollisionTestManager).
- Grep the repo (CLAUDE.md files, skills, docs) for `CollisionTestStateHolder.kt` and fix every path reference in the same commit.

## Behaviour
Verbatim move within one package; nothing else changes.

## Public API
None (examples are unpublished). D32 mirrors this split in `test-collision-noop`.

## Tests
The existing ones (the module has none).

## Verify
`./gradlew :examples:test-collision:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
