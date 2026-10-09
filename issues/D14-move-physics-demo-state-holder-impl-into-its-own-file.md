# Move `PhysicsDemoStateHolderImpl` out of `PhysicsDemoStateHolder.kt` into `PhysicsDemoStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/PhysicsDemoStateHolder.kt`, `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/PhysicsDemoStateHolderImpl.kt` (new)

## Problem
`examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/PhysicsDemoStateHolder.kt` holds `sealed interface PhysicsDemoStateHolder : StateHolder` (:46, with its resource-gate companion) and `internal class PhysicsDemoStateHolderImpl(` (:65–153) plus `private const val LOG_TAG = "Physics"` (:155). One top-level type per file.

## Fix
- Create `examples/demo-physics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPhysics/implementation/PhysicsDemoStateHolderImpl.kt` (license header from the old file, same package).
- Move verbatim: the whole Impl (including the `// The properties below are lazily initialized because we don't need them when we only run the Scene Editor` comment) and `private const val LOG_TAG`.
- The old file keeps the interface and companion. Split the imports so each file keeps exactly what it uses (the Impl takes the actor/state, geometry, manager, `EditableMetadata`, `AngleRadians`, `SceneOffset`, `SceneSize`, `cos`/`sin`/`sceneUnit` and flow imports; `sceneJson` is the same-package `expect val`).
- `PhysicsDemo.kt` and `examples/demo-physics/src/desktopMain/kotlin/com/pandulapeter/kubriko/demoPhysics/PhysicsDemoSceneEditor.kt` import the Impl by package — unaffected.
- Grep the repo for `PhysicsDemoStateHolder.kt`; fix any path reference in the same commit.

## Behaviour
Verbatim move within one package.

## Public API
None (examples are unpublished).

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-physics:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
