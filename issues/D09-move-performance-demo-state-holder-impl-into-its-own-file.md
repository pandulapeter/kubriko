# Move `PerformanceDemoStateHolderImpl` out of `PerformanceDemoStateHolder.kt` into `PerformanceDemoStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/PerformanceDemoStateHolder.kt`, `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/PerformanceDemoStateHolderImpl.kt` (new)

## Problem
`examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/PerformanceDemoStateHolder.kt` holds `sealed interface PerformanceDemoStateHolder : StateHolder` (:32, with its resource-gate companion) and `internal class PerformanceDemoStateHolderImpl(` (:43–99) plus `private const val LOG_TAG = "Performance"` (:101). One top-level type per file.

## Fix
- Create `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/PerformanceDemoStateHolderImpl.kt` (license header from the old file, same package).
- Move verbatim: the whole `internal class PerformanceDemoStateHolderImpl` — including its `// The properties below are lazily initialized because we don't need them when we only run the Scene Editor` comment — and `private const val LOG_TAG`.
- The old file keeps the interface and its companion; split the imports (the Impl takes `Kubriko`, `BoxBody`, `PointBody`, `BoxWithCircle`, `Camera`, `MovingBox`, `PerformanceDemoManager`, `sceneUnit`, `ActorManager`, `ViewportManager`, `EditableMetadata`, `SceneSize`, `MutableStateFlow`, `asStateFlow`; the `sceneJson` it reads is the same-package `expect val` from `PlatformSpecificContent.kt`, no import).
- `examples/demo-performance/src/desktopMain/kotlin/com/pandulapeter/kubriko/demoPerformance/PerformanceDemoSceneEditor.kt` and `PerformanceDemo.kt` import the Impl by package — unaffected.
- Grep the repo for `PerformanceDemoStateHolder.kt`; fix any path reference in the same commit.

## Behaviour
Verbatim move within one package.

## Public API
None (examples are unpublished).

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-performance:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
