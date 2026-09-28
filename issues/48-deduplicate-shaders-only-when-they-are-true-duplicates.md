# De-duplicate shader actors only when they are true duplicates (same class, same layer, same state)

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `plugin-shaders`
**Files:** `plugins/shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/shaders/ShaderManagerImpl.kt`, `plugins/shaders/src/commonTest/kotlin/com/pandulapeter/kubriko/shaders/ShaderDeduplicationTest.kt` (new), `plugins/shaders/CLAUDE.md`

**Decision needed:** which shader actors count as duplicates? — recommended: **same class, same `layerIndex` and equal `shaderState`** (option A below). Changes which shaders render, so it is observable.

## Problem

`ShaderManagerImpl` (at 0008d027) drops every shader whose state *equals* one already seen:

```kotlin
val seenStates = HashSet<Any?>()
allActors.forEach { actor ->
    if (actor is Shader<*>) {
        if (seenStates.add(actor.shaderState)) {
            result.add(actor)
        }
    }
}
```

`HashSet` uses `equals`, and all six built-in states (`VignetteShader.State`, `BlurShader.State`, …) are data classes. So `VignetteShader(layerIndex = 0)` plus `VignetteShader(layerIndex = 1)` — both with the default `State()` — registers only the first: the second layer silently gets no vignette. The same happens across different shader classes whose custom data-class states compare equal. `plugins/shaders/CLAUDE.md` documents the guard as "If two actors share the same `shaderState` object **reference**, only the first is registered", which is not what the code does.

Note: at 0008d027 `game-wallbreaker`'s `GameplayManager` re-adds the **same three shader instances** on every level (examples plan 90 stops that). Both options below still collapse repeated additions of one instance (same class, layer and state — and the same state reference), so Wallbreaker looks the same with or without plan 90.

## Fix

Move the filter into an `internal fun List<Actor>.distinctShaders(): List<Shader<*>>` in `ShaderManagerImpl.kt` (so it can be unit tested without the Main-dispatcher `StateFlow`) and call it from the `map`. It runs only when `allActors` changes, never per frame, so a linear scan over the shaders found so far is fine (shader counts are tiny).

- **A (recommended) — true duplicates only:** skip a shader when an already-kept one has `existing::class == actor::class && existing.layerIndex == actor.layerIndex && existing.shaderState == actor.shaderState`. Two identical shaders on the same layer would apply the same effect twice, which is the case the guard exists for; the same shader on two different layers, or two different shaders with equal-looking states, both render.
- **B — reference identity (what `CLAUDE.md` claims):** skip only when `existing.shaderState === actor.shaderState`. Two separately constructed `VignetteShader()`s on the same layer would then both apply (a visibly darker vignette), which changes any game that adds the same default shader twice.

Rewrite the "Deduplication by shaderState identity" section of `plugins/shaders/CLAUDE.md` to state the chosen rule.

## Tests

`ShaderDeduplicationTest` in `commonTest`, calling `distinctShaders()` on plain lists (no Kubriko instance needed):
- `listOf(VignetteShader(layerIndex = 0), VignetteShader(layerIndex = 1))` → both kept.
- `listOf(VignetteShader(), VignetteShader())` → one kept under A, two under B.
- Two different shader classes (e.g. `VignetteShader()` and `ChromaticAberrationShader()`) → both kept.
- Non-shader actors in the list are ignored; order of the kept shaders follows the input order.

Run `./gradlew :plugins:shaders:desktopTest`.

## Manual check

In `demo-content-shaders`, temporarily add `VignetteShader(layerIndex = <a second layer>)` next to the existing one; run `./gradlew :app:desktop:run` → both layers show the vignette. Revert. Also open Wallbreaker and confirm it looks as before.
