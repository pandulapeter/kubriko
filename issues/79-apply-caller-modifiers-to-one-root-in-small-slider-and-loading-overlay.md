# Apply the caller's modifier only to the root in SmallSlider and LoadingOverlay

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all
**Artifact:** `tool-ui-components`
**Files:** `tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/SmallSlider.kt`, `tools/ui-components/src/commonMain/kotlin/com/pandulapeter/kubriko/uiComponents/LoadingOverlay.kt`, `tools/ui-components/CLAUDE.md`

**Decision needed:** Both public composables apply the caller's `modifier` to a second, inner element as well; removing that changes the layout for any consumer who passes a sizing/padding/background modifier. Apply it to the root only (option A), or keep the current behaviour and document it (option B)? — recommended: option A.

## Problem

`SmallSlider` (~57-58) uses the outer `modifier` both on the `Slider` and on the thumb:

```kotlin
Slider(
    modifier = modifier.height(24.dp),
    ...
    thumb = {
        Spacer(
            modifier
                .size(4.dp, 16.dp)
                .hoverable(interactionSource = interactionSource)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    },
```

A caller's `Modifier.padding(…)`, `.background(…)`, `.fillMaxWidth()` or `.clickable {}` is therefore applied to the 4 × 16 dp thumb too (a padded, recolored or stretched thumb; a second click target). The in-repo callers pass only `Modifier.weight(1f)`, which the thumb ignores, so the Showcase does not show it.

`LoadingOverlay` applies `modifier` to the content `Box` **and** to the `LoadingIndicator`:

```kotlin
Box(
    modifier = modifier,
) {
    content()
}
...
LoadingIndicator(
    modifier = modifier.align(Alignment.BottomStart),
)
```

A caller passing `Modifier.fillMaxSize()` gets a full-screen indicator; one passing padding pads both. The in-repo callers pass no modifier.

## Fix

Option A (recommended):
- `SmallSlider`: the thumb `Spacer` starts from `Modifier` instead of `modifier`.
- `LoadingOverlay`: the content `Box` keeps `modifier`; the overlay `Box` becomes `modifier.fillMaxSize().background(color).padding(16.dp)` so both states occupy the same area the caller asked for; `LoadingIndicator(modifier = Modifier.align(Alignment.BottomStart))`. Update the KDoc `@param modifier` to "applied to the content and to the loading overlay's container".
- `tools/ui-components/CLAUDE.md` table: no change for `SmallSlider`; for `LoadingOverlay` append "`modifier` applies to both the content and the overlay container".

Option B — leave the code and document on both KDocs that `modifier` is also applied to the thumb / indicator. Not recommended; no Compose component behaves like that.

## Tests

None: pure Compose layout, and plan 00 adds no Compose UI test harness.

## Manual check

Temporarily, in `examples/demo-shader-animations/.../ui/ColorSlider.kt`, change one `SmallSlider(modifier = Modifier.weight(1f), …)` to `Modifier.weight(1f).background(Color.Red)`; run the Showcase and open Shader Animations: only the slider's area turns red and the thumb keeps its 4 × 16 dp pill shape. In `PerformanceDemoManager.kt`, pass `modifier = Modifier.padding(32.dp)` to `LoadingOverlay`: while loading, the indicator is 32 dp further in and not stretched. Revert both temporary changes.
