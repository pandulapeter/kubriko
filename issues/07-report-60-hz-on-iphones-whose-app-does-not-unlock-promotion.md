# Report 60 Hz as the iPhone's maximum refresh rate when the app does not unlock ProMotion in its Info.plist

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** iOS (ProMotion iPhones)
**Challenged:** amended — reads the plist flag as `NSNumber` first (with a `Boolean` fallback) instead of relying on `as? Boolean`, whose silent failure would cap every iPhone at 60 including apps that set the key.
**Files:** `engine/src/iosMain/kotlin/com/pandulapeter/kubriko/implementation/PlatformEffects.ios.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/MetadataManager.kt` (KDoc), `engine/CLAUDE.md`, root `CLAUDE.md` (MetadataManager bullet)

Ships in `io.github.pandulapeter.kubriko:engine`. **Changes the value of the public
`MetadataManager.maximumDisplayRefreshRate` on some iPhones** — see Decision.

## Problem

```kotlin
DisposableEffect(Unit) {
    currentOnMaximumDisplayRefreshRateChanged(UIScreen.mainScreen.maximumFramesPerSecond.toFloat())
    onDispose { }
}
```

On iPhone, Core Animation caps every app at 60 Hz unless its `Info.plist` sets
`CADisableMinimumFrameDurationOnPhone = true` (Apple: "Optimizing ProMotion refresh rates for iPhone 13 Pro and
iPad Pro"); `UIScreen.maximumFramesPerSecond` still reports 120 on a ProMotion iPhone. An app built on Kubriko
without the key therefore gets `maximumDisplayRefreshRate == 120` while it can only ever present 60 — the public KDoc
calls this value "the highest refresh rate the display showing the game can present at" and recommends building a
frame rate menu from it, so such a game offers 120/60/40/30 caps of which 120 can never be reached, and a
`Limit(40)` (which divides 120 but not 60) presents unevenly paced. iPads do not need the key.

Both in-repo apps set the key (`app/ios/iosApp/iosApp/Info.plist`, `../Tesselar/app/ios/iosApp/iosApp/Info.plist`),
so neither is affected; third-party consumers who did not are.

## Fix

```kotlin
private fun maximumPresentableFramesPerSecond(): Float {
    val maximum = UIScreen.mainScreen.maximumFramesPerSecond.toFloat()
    val isPhone = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPhone
    val isUnlocked = when (val value = NSBundle.mainBundle.objectForInfoDictionaryKey("CADisableMinimumFrameDurationOnPhone")) {
        is NSNumber -> value.boolValue
        is Boolean -> value
        else -> false
    }
    return if (isPhone && !isUnlocked) minOf(maximum, 60f) else maximum
}
```

(`objectForInfoDictionaryKey` returns the plist boolean as an Objective-C `NSNumber`; a plain `as? Boolean` is not
guaranteed to match it on Kotlin/Native, and if it silently fails every iPhone — including the Showcase and Tesselar,
which set the key — would be capped at 60, so check `NSNumber` first and accept a bridged `Boolean` too.) Read once — the plist cannot
change at runtime. Then:

- `MetadataManager.maximumDisplayRefreshRate` KDoc: add "On iPhone, ProMotion rates above 60 Hz are only reported
  (and only presentable) when the app's `Info.plist` sets `CADisableMinimumFrameDurationOnPhone` to `true`."
- `engine/CLAUDE.md` → Tick Dispatch: "iOS: `UIScreen.maximumFramesPerSecond`" → "…, capped at 60 on an iPhone
  whose `Info.plist` lacks `CADisableMinimumFrameDurationOnPhone`".
- (`engine/README.md` has no iOS setup section, so the KDoc carries the note.) The root `CLAUDE.md` `maximumDisplayRefreshRate`
  bullet gets the same half-sentence.

## Decision

- **A (recommended):** report the presentable ceiling (60) on an iPhone without the key, and document the key —
  matches the KDoc's contract ("can present at"); only apps that omit the key see a different value, and that value
  is the true one.
- **B:** keep reporting `maximumFramesPerSecond` and only document the key in the KDoc/README.
- **C:** A plus a one-time engine log (`isLoggingEnabled`) telling the developer the key is missing.

## Tests

None: depends on `UIScreen`, `UIDevice` and the main bundle; the iOS test tasks are disabled.

## Manual check

On a ProMotion iPhone (13 Pro or later): run the Showcase (which sets the key) and confirm the value is 120; remove
`CADisableMinimumFrameDurationOnPhone` from `app/ios/iosApp/iosApp/Info.plist`, run again — it reports 60 (today: 120).
On a ProMotion iPad it reports 120 either way.
