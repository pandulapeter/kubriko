# Move `AudioTestStateHolderImpl` out of `AudioTestStateHolder.kt` into `AudioTestStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/AudioTestStateHolder.kt`, `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/AudioTestStateHolderImpl.kt` (new), `examples/test-audio/CLAUDE.md`
**Challenged:** amended — fixed the mangled Gradle task in Verify (`:examples:test-audio:compileKotlinDesktop`).

## Problem
`examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/AudioTestStateHolder.kt` holds `sealed interface AudioTestStateHolder : StateHolder` (:36, with its `companion object` resource gate) and `internal class AudioTestStateHolderImpl` (:60–90) plus `private const val LOG_TAG = "Audio"` (:92). One top-level type per file, named after it.

## Fix
- Create `examples/test-audio/src/commonMain/kotlin/com/pandulapeter/kubriko/testAudio/implementation/AudioTestStateHolderImpl.kt` (MPL-2.0 header copied from the old file, same package) and move the whole `internal class AudioTestStateHolderImpl` verbatim together with the file-private `LOG_TAG` (stays `private`; only the Impl uses it).
- The old file keeps only the interface and its companion; split the imports so each file keeps exactly what it uses.
- A sealed interface may be implemented in another file of the same package and module; callers import the Impl by package, which does not change.
- In `examples/test-audio/CLAUDE.md`'s "Module structure" tree, replace the `AudioTestStateHolder.kt — sealed interface + Impl; …` line with two lines: `AudioTestStateHolder.kt` (sealed interface + resource gate) and `AudioTestStateHolderImpl.kt` (creates MusicManager, SoundManager, AudioTestManager).
- Grep the repo (CLAUDE.md files, skills, docs) for `AudioTestStateHolder.kt` and fix every path reference in the same commit.

## Behaviour
Verbatim move within one package; nothing else changes.

## Public API
None (examples are unpublished). D29 mirrors this split in `test-audio-noop`.

## Tests
The existing ones (the module has none).

## Verify
`./gradlew :examples:test-audio:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
