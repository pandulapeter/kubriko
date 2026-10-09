# Draw warming-up sprites with a tiny nonzero alpha into a 1×1 destination, so Skia does not discard the warm-up draw.

**Kind:** performance  ·  **Severity:** low  ·  **Platforms:** Desktop, iOS, Web (harmless on Android)
**Challenged:** sound
**Files:** `plugins/sprites/src/commonMain/kotlin/com/pandulapeter/kubriko/sprites/SpriteManagerImpl.kt`, `plugins/sprites/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-sprites`. No API change.

## Problem
After decoding, a sprite waits in `pendingWarmingUp` and is drawn once by the manager's `Composable()` so its texture reaches the GPU before the first visible use (the module's `CLAUDE.md`, "Warm-up phase"). The draw uses alpha 0:

```kotlin
Canvas(
    modifier = Modifier.fillMaxSize(),
    onDraw = {
        pending.values.forEach { bitmap ->
            drawImage(
                image = bitmap,
                alpha = 0f,
            )
        }
    }
)
```

Skia discards such a draw before it reaches the device. `SkCanvas::internalQuickReject` returns early when `paint.nothingToDraw()`, which is true for a source-over paint with alpha 0. On the Skia targets, Compose passes the alpha straight to the Skia paint (`SkiaBackedCanvas.drawImageRect` → `Image.makeFromBitmap(bitmap)` → `internalSkiaCanvas.drawImageRect(..., paint.asSkiaPaintWithAppliedAlphaMultiplier())`), so the image is never touched and nothing is uploaded. The first visible draw of every sprite then pays the upload, which is a hitch for a large sprite sheet. That is the cost the warm-up is there to avoid.

Verified with the Skiko 0.150.1 native runtime used by the project (throwaway probes `AlphaProbe.java` and `TinyProbe.java` in the sweep scratchpad, `/private/tmp/claude-501/-Users-pandulapeter-Projects-Kubriko/d008fbb8-e9ac-41bd-bd4b-41979f05ab2d/scratchpad/writer-R/`, may be gone). A lazily decoded image is decoded, so it enters Skia's resource cache, only when a draw is not rejected:

```
alpha=0 resourceCacheUsed 0 -> 0
alpha=1 resourceCacheUsed 0 -> 1048624
alpha=0 resourceCacheUsed 1048624 -> 1048624
alpha=255 resourceCacheUsed 1048624 -> 2097248
1x1 dst alpha=3: 0 -> 1048624
```

The rejection happens in the `SkCanvas` base class before any device (raster or GPU) is involved, so on GPU surfaces the alpha-0 draw also uploads nothing. On Android, HWUI records the bitmap into the display list and pins it as a texture when the frame syncs, whatever the paint, so the warm-up probably works there today. The fix does not change that.

## Fix
In `SpriteManagerImpl.Composable()`, draw each pending bitmap's full source into a 1×1 destination at the canvas origin with the smallest alpha that does not round to 0:

```kotlin
drawImage(
    image = bitmap,
    srcOffset = IntOffset.Zero,
    srcSize = IntSize(bitmap.width, bitmap.height),
    dstOffset = IntOffset.Zero,
    dstSize = IntSize(1, 1),
    alpha = WARM_UP_ALPHA,
)
```

with `private const val WARM_UP_ALPHA = 1f / 255f` in the companion (next to `WARM_UP_TIMEOUT_MS`). Both Compose backends round the float alpha to the nearest 8-bit value (Skiko: `Color.copy(alpha = …).toArgb()`, Android: `round(value * 255)`), so `1f / 255f` becomes 1 and is not rejected. The source rect has to be the full image (the probe shows a 1×1 destination still decodes and uploads the whole image). The destination has to stay inside the canvas, because a draw outside the clip is quick-rejected the same way. The visible result is at most one level of difference in one pixel for one frame. `IntOffset`/`IntSize` are value classes, so this adds no allocation, and the canvas only exists while something is pending.

Update `plugins/sprites/CLAUDE.md`, "Warm-up phase": replace "drawn once via an invisible `Canvas` (alpha=0)" with the 1×1, 1/255-alpha draw and one sentence on why alpha 0 does not work (Skia quick-rejects draws with a fully transparent source-over paint).

## Tests
None can be written: library test classpaths have no native Skia runtime and no GPU, and what changes is whether a texture upload happens. The existing `SpriteManagerTest`/`SpriteManagerRetryTest` cover the `pendingWarmingUp` → cache promotion, which this does not change; run `./gradlew :plugins:sprites:desktopTest`.

## Manual check
Desktop (and ideally the web build): run the Showcase, open a sprite-heavy game for the first time in the session (Space Squadron), and confirm that no stray pixel shows at the viewport's top-left corner while sprites load. Optionally compare the first gameplay frames' frame times before and after the change in a profiler. A hitch on a sprite's first appearance should be gone or smaller.
