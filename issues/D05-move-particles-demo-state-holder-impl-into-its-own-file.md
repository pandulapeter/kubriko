# Move `ParticlesDemoStateHolderImpl` out of `ParticlesDemoStateHolder.kt` into `ParticlesDemoStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ParticlesDemoStateHolder.kt`, `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ParticlesDemoStateHolderImpl.kt` (new)

## Problem
`examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ParticlesDemoStateHolder.kt` holds `sealed interface ParticlesDemoStateHolder : StateHolder` (:31, with the `companion object` resource gate) and `internal class ParticlesDemoStateHolderImpl(` (:51–80) plus `private const val LOG_TAG = "Particles"` (:82). One top-level type per file.

## Fix
- Create `examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/ParticlesDemoStateHolderImpl.kt` (license header from the old file, same package).
- Move verbatim: the whole `internal class ParticlesDemoStateHolderImpl` and `private const val LOG_TAG` (stays private; only the Impl uses it).
- The old file keeps the interface and its companion.
- Split the imports (Impl file: `Kubriko`, `ParticlesDemoManager`, `ParticleManager`, `MutableStateFlow`, `asStateFlow`; interface file keeps `Composable`, `StateHolder`, `preloadedImageVector`, `preloadedString`, `Res` and the resource accessors).
- Grep the repo for `ParticlesDemoStateHolder.kt`; fix any path reference in the same commit.

## Behaviour
Verbatim move within one package.

## Public API
None (examples are unpublished).

## Tests
The existing ones (none in the module).

## Verify
`./gradlew :examples:demo-particles:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
