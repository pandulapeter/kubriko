# Apply `BlurShader` from Android 12 (API 31), where its native blur already exists, instead of only from Android 13.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Android 12 / 12L (API 31–32)
**Challenged:** sound
**Files:** `plugins/shaders/src/androidMain/kotlin/com/pandulapeter/kubriko/shaders/extensions/ModifierExtensions.android.kt`, `plugins/shaders/CLAUDE.md`, `plugins/shaders/README.md`, `plugins/shaders/src/commonMain/kotlin/com/pandulapeter/kubriko/shaders/collection/BlurShader.kt` (KDoc)

Ships in `io.github.pandulapeter.kubriko:plugin-shaders`.

## Decision
On Android 12/12L devices a `BlurShader` that currently does nothing starts blurring. That is visible to players and to game code: Annoyed Penguins' pause blur (`GradualBlurShader`, added while `StateManager.isRunning` is false) starts working there. The Content Shaders demo stays behind its `areShadersSupported` guard.

- **A — enable blur from API 31 (recommended).** This is what `BlurShader`'s KDoc already promises ("uses native blur implementations where available"). `areShadersSupported` stays `false` below API 33, since SKSL shaders still need `RuntimeShader`.
- **B — keep API 33 for everything** and document that `BlurShader` needs Android 13 too.

## Problem
`BlurShader` is a `ContentShader`, so `Modifier.shader` sets it as the `graphicsLayer`'s `renderEffect` through `createRenderEffect`. On Android that function puts the whole body, blur included, behind the `RuntimeShader` API level:

```kotlin
internal actual fun <T : Shader.State> createRenderEffect(
    shader: Shader<T>,
    size: Size,
): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (shader is BlurShader) {
            return ... RenderEffect.createBlurEffect(blurHorizontal, blurVertical, <TileMode>).asComposeRenderEffect()
        } else { ... RuntimeShader path ... }
    } else {
        return null
    }
}
```

The blur branch uses no SKSL. `android.graphics.RenderEffect.createBlurEffect` and `Shader.TileMode.DECAL` are API 31. Compose applies a layer's render effect from API 31 as well. In `GraphicsLayerV29` (compose ui-graphics-android 1.12.1, the version in use):

```kotlin
override var renderEffect: RenderEffect? = null
    set(value) {
        field = value
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderNodeVerificationHelper.setRenderEffect(renderNode, value)
        }
    }
```

So on API 31–32 the blur would render through exactly the same `graphicsLayer { renderEffect = … }` path, but the gate returns `null` and the content is drawn unblurred. The project's `minSdk` is 29, so these are supported devices.

## Fix
Restructure `createRenderEffect` in `ModifierExtensions.android.kt`:

```kotlin
if (shader is BlurShader) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return createBlurRenderEffect(shader)
}
if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
// existing RuntimeShader path, unchanged
```

and move the blur construction (the radius check returning `null` for `<= 0`, the `Mode` → `TileMode` mapping, `asComposeRenderEffect()`) into a `@RequiresApi(Build.VERSION_CODES.S)` private function, so lint can check the API levels. Leave `drawGenerativeShader` and `areShadersSupported` alone: `BlurShader` is never generative, and SKSL still needs API 33.

Documentation:
- `plugins/shaders/CLAUDE.md`, "Platform support" → Android: SKSL shaders need API 33. `BlurShader` works from API 31 (native `RenderEffect.createBlurEffect`), and below that it has no effect. `areShadersSupported` reports SKSL support only, so it is `false` on API 31–32 even though blur works.
- `BlurShader` KDoc: add "On Android it requires API 31; elsewhere it is always available."
- `plugins/shaders/README.md`: add a short "Platform support" note with the same two facts.

(Unrelated but in the same README: the "Use Built-in Shaders" snippet `BlurShader(radius = 10f)` / `VignetteShader(intensity = 0.8f)` and the `MyEffect : Actor()` / `setFloatUniform` example do not match the API. Fix them only if the executing agent is already editing that file, and keep the change to the parameter names.)

## Tests
None: the branch depends on `Build.VERSION.SDK_INT` and Android framework classes, and the library's tests run on the desktop JVM only. Compile with `./gradlew :plugins:shaders:build` (the Android target and lint check the `@RequiresApi` usage).

## Manual check
On an Android 12 or 12L device or emulator (API 31/32): in the Showcase, open Annoyed Penguins and pause the game. The scene behind the pause menu should now blur in gradually. (The Content Shaders demo cannot show this, because it checks `areShadersSupported` and shows its "not supported" message below API 33.) On API 33+ nothing changes, and on API 29–30 the pause screen still does not blur.
