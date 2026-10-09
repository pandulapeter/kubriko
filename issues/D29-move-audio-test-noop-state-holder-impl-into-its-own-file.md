# Move `AudioTestStateHolderImpl` out of `AudioTestStateHolder.kt` into `AudioTestStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-audio-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/AudioTestStateHolder.kt`, `examples/test-audio-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/AudioTestStateHolderImpl.kt` (new), `examples/test-audio-noop/CLAUDE.md`

## Problem
`examples/test-audio-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/AudioTestStateHolder.kt` holds `sealed interface AudioTestStateHolder : StateHolder` (:17, with its `companion object` resource gate) and `internal class AudioTestStateHolderImpl` (:25–30). One top-level type per file, named after it.

## Fix
- Create `examples/test-audio-noop/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/AudioTestStateHolderImpl.kt` (MPL-2.0 header copied from the old file, same package) and move the whole `internal class AudioTestStateHolderImpl` verbatim.
- The old file keeps only the interface and its companion; split the imports so each file keeps exactly what it uses.
- A sealed interface may be implemented in another file of the same package and module; callers import the Impl by package, which does not change.
- In `examples/test-audio-noop/CLAUDE.md`, "Two files that mirror…" becomes "Three files…", and the `AudioTestStateHolder.kt` bullet is split into an `AudioTestStateHolder.kt` bullet (interface, `areResourcesLoaded()` always `true`) and an `AudioTestStateHolderImpl.kt` bullet (`kubriko = emptyFlow<Kubriko?>()`, no-op `dispose()`).
- Grep the repo (CLAUDE.md files, skills, docs) for `AudioTestStateHolder.kt` and fix every path reference in the same commit.

## Behaviour
Verbatim move within one package; nothing else changes.

## Public API
None (examples are unpublished). The noop must keep mirroring `test-audio`'s file layout and API after D28.

## Tests
The existing ones (the module has none).

## Verify
`./gradlew :examples:test-audio-noop:compileKotlinDesktop` (the Showcase links the noop only with `showcase.areTestExamplesEnabled=false`)

## Manual check
none
