# Stop detecting non-Apple touch browsers as iPhones and iPads, and compare the window's aspect ratio as a float

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** Web
**Challenged:** amended — keeps the `!contains("Chrome")` guard in the touch fallback instead of dropping it (no downside, still excludes a Chromium desktop-mode Mac user agent on a touch device) and adds that test case.
**Files:** `engine/src/webMain/kotlin/com/pandulapeter/kubriko/implementation/BrowserDetection.kt`, optionally a new `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/implementation/AppleDeviceDetection.kt` and `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/implementation/AppleDeviceDetectionTest.kt`, `engine/CLAUDE.md` (only if the file list changes)

Ships in `io.github.pandulapeter.kubriko:engine`. **The public `Window.isRunningOnIphone()` /
`Window.isRunningOnIpad()` return different results** for the browsers they currently misclassify — see Decision.

## Problem

```kotlin
fun Window.isRunningOnIphone() =
    navigator.userAgent.contains("iPhone") || (!navigator.userAgent.contains("Chrome") && navigator.maxTouchPoints > 0 && window.innerWidth / window.innerHeight > 1.6)

fun Window.isRunningOnIpad() =
    navigator.userAgent.contains("iPad") || (!navigator.userAgent.contains("Chrome") && navigator.maxTouchPoints > 0 && window.innerWidth / window.innerHeight <= 1.6)
```

The fallback exists for iPadOS (and iPhone "Request Desktop Website") Safari, which sends a desktop macOS user agent
(`Macintosh; Intel Mac OS X …`) with touch points. But it matches **any** touch browser without "Chrome" in its user
agent:

- **Firefox for Android** (`Mozilla/5.0 (Android 14; Mobile; rv:…) Gecko/… Firefox/…`): in portrait
  `innerWidth / innerHeight` is `0` → "iPad"; in landscape a ~2.1 ratio is `2` → "iPhone".
- Firefox (or any non-Chromium browser) on a **Windows/Linux touch laptop**: "iPad".
- `innerWidth` and `innerHeight` are `Int` in Kotlin's `org.w3c.dom.Window`, so the ratio is **integer division**:
  `> 1.6` really means `≥ 2` and `<= 1.6` means `< 2`. A 16:9 window (1.77) is classified as not-wide.
- Both functions are extensions on `Window` but read the global `kotlinx.browser.window` instead of the receiver.

Consequences in the Showcase (the only in-repo callers): the welcome screen shows the iPhone/iPad Safari disclaimer
(`Disclaimer.web.kt` checks `isRunningOnIphone()` then `isRunningOnIpad()` before the "not Chrome or Firefox" one), and
`KubrikoShowcaseApp.kt` (web) disables the fullscreen toggle when `isRunningOnIphone()` (`mutableStateOf(if
(window.isRunningOnIphone()) null else false)`) — so Firefox Android users opening it in landscape lose fullscreen.

Callers: `app/web/.../KubrikoShowcaseApp.kt`, `app/shared/src/webMain/.../ui/Disclaimer.web.kt`; none in Tesselar
(`../Tesselar` greps clean).

## Fix

Require the macOS user agent for the touch fallback, compare as a float, and use the receiver:

```kotlin
fun Window.isRunningOnIphone() =
    navigator.userAgent.contains("iPhone") || (isAppleTouchDeviceWithDesktopUserAgent() && aspectRatio() > 1.6f)

fun Window.isRunningOnIpad() =
    navigator.userAgent.contains("iPad") || (isAppleTouchDeviceWithDesktopUserAgent() && aspectRatio() <= 1.6f)

private fun Window.isAppleTouchDeviceWithDesktopUserAgent() =
    navigator.userAgent.contains("Macintosh") && !navigator.userAgent.contains("Chrome") && navigator.maxTouchPoints > 0

private fun Window.aspectRatio() = innerWidth.toFloat() / innerHeight.coerceAtLeast(1)
```

Keep the existing `!contains("Chrome")` condition: it costs nothing and still excludes a Chromium browser on a touch
device that sends a macOS user agent in its desktop-site mode (iPad Chrome's desktop user agent carries no "Chrome"
token, so iPads are unaffected). Macs report
`maxTouchPoints == 0`, so desktop Safari stays unaffected. Update the KDoc of both functions ("a touch screen with a
macOS user agent — iPadOS Safari and desktop-mode Safari — with a wide/not-that-wide window").

To make it testable (recommended), put the classification in a pure internal function in `commonMain`, e.g.
`internal fun classifyAppleDevice(userAgent: String, maxTouchPoints: Int, width: Int, height: Int): AppleDevice?`
(`AppleDevice` an internal enum `Iphone`, `Ipad`), and have the two public web functions delegate to it. The public
functions stay in `BrowserDetection.kt` (their JVM-less facade does not matter on Wasm, but the signatures and package
must stay).

## Decision

The public functions are in a published artifact. Options:

- **A (recommended):** fix them in place. Results change only for browsers that are not Apple devices (Firefox
  Android, non-Chromium touch laptops) and for iPad/iPhone desktop-mode windows with a ratio between 1.6 and 2, which
  were misclassified by the integer division.
- **B:** leave the engine functions as they are and fix the detection in `app/` only — keeps the wrong answers for any
  external caller.
- **C:** A, plus deprecate the functions in favour of moving them into the Showcase (they exist for its web shell) —
  an API removal later; not needed for the bug.

## Tests

With the pure function from the Fix: `AppleDeviceDetectionTest` in `engine/src/desktopTest/.../implementation/`
(commonTest also runs on the JVM, either works), cases:

- Firefox Android UA, touch 5, 412×915 → `null`; same at 915×412 → `null`.
- iPhone Safari UA (`iPhone`) → `Iphone`; iPad UA (`iPad`) → `Ipad`.
- `Macintosh` UA, touch 5, 1180×820 → `Ipad`; `Macintosh`, touch 5, 852×393 (2.17) → `Iphone`;
  `Macintosh`, touch 5, 1700×1000 (1.7) → `Iphone` (fails today via integer division: `1 > 1.6` is false).
- `Macintosh` UA, touch 0 (desktop Safari) → `null`; Windows Firefox UA, touch 10 → `null`; a `Macintosh` UA
  containing `Chrome/`, touch 5 → `null`.

Without the extraction, no test is possible (the web test tasks are disabled and the functions read browser globals).

## Manual check

Open the web Showcase in Firefox on an Android phone in portrait and in landscape: no iPhone/iPad disclaimer, and the
fullscreen toggle works. On an iPad (Safari) and an iPhone (Safari, also with "Request Desktop Website") the matching
disclaimer still appears.
