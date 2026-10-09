# Move `InputTestStateHolderImpl` out of `InputTestStateHolder.kt` into `InputTestStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-input-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/InputTestStateHolder.kt`, `examples/test-input-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/InputTestStateHolderImpl.kt` (new), `examples/test-input-noop/CLAUDE.md`

## Problem
`examples/test-input-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/InputTestStateHolder.kt` holds `sealed interface InputTestStateHolder : StateHolder` (:17, with its `companion object` resource gate) and `internal class InputTestStateHolderImpl` (:25–30). One top-level type per file, named after it.

## Fix
- Create `examples/test-input-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/InputTestStateHolderImpl.kt` (MPL-2.0 header copied from the old file, same package) and move the whole `internal class InputTestStateHolderImpl` verbatim.
- The old file keeps only the interface and its companion; split the imports so each file keeps exactly what it uses.
- A sealed interface may be implemented in another file of the same package and module; callers import the Impl by package, which does not change.
- In `examples/test-input-noop/CLAUDE.md`, "Two files that mirror…" becomes "Three files…" and the `InputTestStateHolder.kt` bullet is split into the interface bullet and an `InputTestStateHolderImpl.kt` bullet.
- Grep the repo (CLAUDE.md files, skills, docs) for `InputTestStateHolder.kt` and fix every path reference in the same commit.

## Behaviour
Verbatim move within one package; nothing else changes.

## Public API
None (examples are unpublished). The noop must keep mirroring `test-input`'s file layout and API after D34.

## Tests
The existing ones (the module has none).

## Verify
`./gradlew :examples:test-input-noop:compileKotlinDesktop` (the Showcase links the noop only with `showcase.areTestExamplesEnabled=false`)

## Manual check
none
