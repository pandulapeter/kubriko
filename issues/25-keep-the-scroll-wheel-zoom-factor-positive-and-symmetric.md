# Keep the scroll-wheel zoom factor positive and symmetric on every platform

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Web (most exposed), Desktop, iOS (pointer/trackpad scrolling)
**Challenged:** amended — the exponent is clamped, because a Float `exp` underflows to 0 below about -103 and overflows to Infinity above about 88, so the unclamped formula still produced 0 (and the plan's own `scrollZoomFactor(10_000f, 0.05f) > 0f` test failed against it).
**Files:** `plugins/pointer-input/src/commonMain/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/PlatformExtensions.kt`, `plugins/pointer-input/src/webMain/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/PlatformExtensions.web.kt`, `plugins/pointer-input/src/desktopMain/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/PlatformExtensions.desktop.kt`, `plugins/pointer-input/src/iosMain/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/PlatformExtensions.ios.kt`, new `plugins/pointer-input/src/desktopTest/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/ScrollZoomFactorTest.kt`, `plugins/pointer-input/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-pointer-input`. Internal; `onPointerZoom`'s documented contract
("Values > 1 mean zoom in, values < 1 mean zoom out") is what the fix enforces.

## Problem

The scroll wheel is turned into an `onPointerZoom` factor linearly:

```kotlin
// PlatformExtensions.web.kt
.onPointerEvent(PointerEventType.Scroll) {
    onZoomDetected(it.changes.first().position, 1f - it.changes.first().scrollDelta.y * 0.005f)
}
// PlatformExtensions.desktop.kt and PlatformExtensions.ios.kt: the same with * 0.05f
```

- **The factor reaches zero or goes negative.** Compose web passes the browser's raw `WheelEvent.deltaY` as
  `scrollDelta.y` and ignores `deltaMode` (`ComposeWindowInternal.web.kt`, `onWheelEvent`: `verticalScroll =
  event.deltaY`). Chrome reports ~100 px per notch, so one notch already halves the scale (factor 0.5); a single
  event of ≥ 200 px — Windows with "lines to scroll" raised, a free-spinning wheel, a page-mode wheel converted by the
  browser — gives a factor ≤ 0. On Desktop, Compose passes AWT's `preciseWheelRotation` (~1 per notch, but coalesced
  fast spins and macOS acceleration reach 20+), and 20 gives 0. A non-positive factor is not a zoom: the engine's
  `multiplyScaleFactor` clamps it to the minimum scale (a sudden snap to fully zoomed out), and consumers that take
  its logarithm break — Tesselar's `RegionPreviewCameraManager` already carries a guard for exactly this ("A
  non-positive factor (a platform gesture edge case) would take ln() to NaN").
- **It is asymmetric.** `(1 - d·k)·(1 + d·k) = 1 - d²k² < 1`, so scrolling in and back out by the same amount does not
  return to the starting scale (one Chrome notch in and out: 0.5 × 1.5 = 0.75).
- **Line and page delta modes are not normalized on the web**: a browser that reports `DOM_DELTA_LINE` (deltaY ≈ 3 per
  notch) zooms ~30× slower than one reporting pixels.

## Fix

1. Add to commonMain `implementation/PlatformExtensions.kt`:
   ```kotlin
   internal fun scrollZoomFactor(scrollDelta: Float, sensitivity: Float): Float = exp(-scrollDelta * sensitivity)
   ```
   `f(d) · f(-d) == 1`, and for small deltas within a few percent of the old linear factor (desktop one notch: 0.951
   vs 0.95), so the feel does not change where the old formula was sane. Guard non-finite input first
   (`if (!scrollDelta.isFinite()) return 1f`), then **clamp the exponent symmetrically** before calling `exp`:
   `exp((-scrollDelta * sensitivity).coerceIn(-MAX_ZOOM_EXPONENT, MAX_ZOOM_EXPONENT))` with
   `private const val MAX_ZOOM_EXPONENT = 10f` (a factor between ~4.5e-5 and ~22026, far beyond any scale range the
   engine allows). Without the clamp a Float `exp` returns `0f` for exponents below about -103 and `Infinity` above
   about 88 — a 10 000-unit coalesced desktop spin is exponent -500 — which re-creates the "factor 0 snaps to fully
   zoomed out" bug and hands Tesselar's `ln(factor)` an infinity. The clamp keeps the function positive, finite and
   symmetric.
2. Desktop and iOS: `onZoomDetected(position, scrollZoomFactor(scrollDelta.y, 0.05f))`.
3. Web: normalize to pixels first, reading `deltaMode` from the native event:
   `val wheelEvent = it.nativeEvent as? org.w3c.dom.events.WheelEvent` (Compose web puts the DOM `WheelEvent` in
   `PointerEvent.nativeEvent`); `deltaMode` `0` → as is, `1` (lines) → × `100f / 3f` (Chrome's own line height for a
   3-line notch), `2` (pages) → × the viewport's height in pixels (or a constant `800f`); unknown/null event → as is.
   Then `scrollZoomFactor(pixels, 0.005f)` — keeps today's per-pixel sensitivity (one Chrome notch: 0.61 instead
   of 0.5). If the `as?` cast on the Wasm external type does not compile or always fails, skip the normalization:
   step 1 alone fixes the ≤ 0 and asymmetry bugs.
4. `CLAUDE.md` "Multi-touch and platform differences": replace the "Scroll-to-zoom factor formula" bullet with
   "`exp(-delta · k)`, k = 0.05 per wheel notch on Desktop/iOS and 0.005 per pixel on the Web (line/page delta modes
   converted to pixels first); always > 0 and symmetric."

No allocation is added: the per-event code reads two floats.

## Tests

`ScrollZoomFactorTest` (desktopTest; the function is internal to the module, so the test sits in the same package):
- `factorIsPositiveAndFiniteForLargeDeltas`: `scrollZoomFactor(10_000f, 0.05f)` and `scrollZoomFactor(-10_000f, 0.05f)`
  are `> 0f` and finite, and `scrollZoomFactor(200f, 0.005f) > 0f` (the first two pin the clamp: they give `0f` and
  `Infinity` without it).
- `largeOppositeDeltasStillCancel`: `scrollZoomFactor(10_000f, 0.05f) * scrollZoomFactor(-10_000f, 0.05f)` equals 1
  within 1e-3 (relative).
- `oppositeDeltasCancel`: for d in `[0.5f, 1f, 3f, 100f]`, `scrollZoomFactor(d, k) * scrollZoomFactor(-d, k)` equals 1 within 1e-5.
- `scrollingDownZoomsOut`: `scrollZoomFactor(1f, 0.05f) < 1f`, `scrollZoomFactor(-1f, 0.05f) > 1f`.
- `zeroAndNonFiniteDeltasDoNotZoom`: `0f`, `NaN`, `Infinity` → `1f`.

## Manual check

Web (Chrome, Firefox, Safari) and Desktop: in `demo-isometric-graphics` (wheel zoom) spin the wheel as fast as
possible — the view must never snap to fully zoomed out; scroll N notches in and N out — the scale must return to
where it started. One notch must still feel roughly as it did.
