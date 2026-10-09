# Move `InputTestStateHolderImpl` out of `InputTestStateHolder.kt` into `InputTestStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/InputTestStateHolder.kt`, `examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/InputTestStateHolderImpl.kt` (new), `examples/test-input/CLAUDE.md`
**Challenged:** amended — fixed the mangled Gradle task in Verify (`:examples:test-input:compileKotlinDesktop`).

## Problem
`examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/InputTestStateHolder.kt` holds `sealed interface InputTestStateHolder : StateHolder` (:27, with its `companion object` resource gate) and `internal class InputTestStateHolderImpl` (:38–69) plus `private const val LOG_TAG = "Input"` (:71). One top-level type per file, named after it.

## Fix
- Create `examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/InputTestStateHolderImpl.kt` (MPL-2.0 header copied from the old file, same package) and move the whole `internal class InputTestStateHolderImpl` verbatim together with the file-private `LOG_TAG` (stays `private`; only the Impl uses it).
- The old file keeps only the interface and its companion; split the imports so each file keeps exactly what it uses.
- A sealed interface may be implemented in another file of the same package and module; callers import the Impl by package, which does not change.
- In `examples/test-input/CLAUDE.md`'s "Module structure" tree, split the `InputTestStateHolder.kt — sealed interface + Impl; creates …` entry into `InputTestStateHolder.kt` (sealed interface + resource gate) and `InputTestStateHolderImpl.kt` (creates PointerInputManager, KeyboardInputManager, GamepadInputManager and InputTestManager).
- Grep the repo (CLAUDE.md files, skills, docs) for `InputTestStateHolder.kt` and fix every path reference in the same commit.

## Behaviour
Verbatim move within one package; nothing else changes.

## Public API
None (examples are unpublished). D35 mirrors this split in `test-input-noop`.

## Tests
The existing ones (the module has none).

## Verify
`./gradlew :examples:test-input:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
