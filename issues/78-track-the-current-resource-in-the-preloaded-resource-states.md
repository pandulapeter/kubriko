# Track the currently resolved resource in the preloaded image, vector and font states

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Android, desktop, iOS (web already correct)
**Artifact:** `tool-ui-components`
**Files:** `tools/ui-components/src/androidMain/kotlin/com/pandulapeter/kubriko/uiComponents/utilities/ResourceLoaders.android.kt`, `tools/ui-components/src/desktopMain/kotlin/com/pandulapeter/kubriko/uiComponents/utilities/ResourceLoaders.desktop.kt`, `tools/ui-components/src/iosMain/kotlin/com/pandulapeter/kubriko/uiComponents/utilities/ResourceLoaders.ios.kt`

## Problem

On the three non-web targets the loaders wrap an already resolved value in a `derivedStateOf` that reads no state:

```kotlin
@Composable
actual fun preloadedImageBitmap(
    resource: DrawableResource,
): State<ImageBitmap?> {
    val image = imageResource(resource)
    return remember(resource) { derivedStateOf { image } }
}
```

(`preloadedImageVector` identical; desktop `preloadedFont` does the same with `remember(resource)`; Android/iOS `preloadedFont` use `remember(font)`, which is fine.) `derivedStateOf` only recomputes when a *state* it read changes; `image` is a captured local, so the returned `State` keeps the value from the first composition for that `resource`. `imageResource`/`vectorResource` re-resolve on configuration changes — dark/light theme, density or language qualifiers — and the new variant is dropped. On desktop, calling `preloadedFont(resource, weight = …)` with a different `weight` or `style` for the same resource keeps returning the first `Font`.

This repository has no qualified drawable folders, so the Showcase does not show it; consumers of the published `tool-ui-components` with `drawable-dark` (or density) variants get the stale variant after a theme switch. The function names and KDoc promise the current resource; the returned type does not change, so this is a fix rather than a decision.

## Fix

Replace each `remember(…) { derivedStateOf { x } }` with `rememberUpdatedState(x)` (same `State<T>` type; `State` is covariant, so `State<ImageBitmap>` satisfies `State<ImageBitmap?>`):
- Android, desktop, iOS: `preloadedImageBitmap`, `preloadedImageVector`;
- desktop: `preloadedFont` (Android/iOS `preloadedFont` may use it too, for uniformity).

Remove the now-unused `derivedStateOf` and `remember` imports. The web actuals delegate to Compose's own `preload*` functions and stay unchanged.

## Tests

None: the functions are Composables whose behaviour shows only under recomposition with a changed resource environment; plan 00 adds no Compose test harness.

## Manual check

Desktop: temporarily add a `drawable-dark/` variant of one Showcase icon with an obviously different color (e.g. a copy of `app/shared`'s about-screen icon recolored red), run `./gradlew :app:desktop:run`, and switch the OS theme between light and dark while the Showcase is open: the icon switches variant (before the fix it keeps the first one). Remove the temporary resource.
