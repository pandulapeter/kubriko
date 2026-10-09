# Remember the filtered `gameTime` flow so the performance mini map stops restarting its collection every frame, and fix the CLAUDE.md claim.

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/managers/PerformanceDemoManager.kt`, `examples/demo-performance/CLAUDE.md`
**Challenged:** amended — the CLAUDE.md rewording names the scope that actually recomposes per sampled tick (the `Panel` content, not the whole AnimatedVisibility content).

## Problem
`examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/managers/PerformanceDemoManager.kt:107`, inside the `AnimatedVisibility` content:
```kotlin
gameTime = metadataManager.totalRuntimeInMilliseconds.filter { it % 2 == 0L }.collectAsState(0L).value,
```
builds a new `Flow` on every composition. `collectAsState` is `produceState` keyed on the flow instance, and this scope recomposes on every value it emits, so each sampled tick cancels the collector and launches a new one (plus a new `filter` operator) — per-frame coroutine churn in a demo whose purpose is measuring performance. `examples/demo-performance/CLAUDE.md` ("**Mini-map rendering without flow subscriptions.**") claims this approach "avoids re-composing the mini-map's parent on every frame", which is false: `gameTime` is read in that parent (the AnimatedVisibility content) and recomposes it each sampled tick.

## Fix
- Change the line to `gameTime = remember { metadataManager.totalRuntimeInMilliseconds.filter { it % 2 == 0L } }.collectAsState(0L).value,` (import `androidx.compose.runtime.remember`). Keep the `it % 2 == 0L` filter as is.
- In CLAUDE.md, reword the last sentence of that paragraph: the mini map's `Panel` content (inside the `AnimatedVisibility`; `Panel`'s content lambda is its own recompose scope) recomposes once per sampled tick to pass `gameTime`, and the lambda callbacks avoid collecting and snapshotting the actor lists.

## Behaviour
`produceState`'s value holder is not keyed, so the same values reach `MiniMap` in the same order; only the per-tick collector restart disappears.

## Public API
None.

## Tests
None possible without Compose UI tests (none exist in examples).

## Verify
`./gradlew :examples:demo-performance:compileKotlinDesktop`

## Manual check
Open Performance: the mini map still animates every frame and the red viewport rectangle tracks the camera.
