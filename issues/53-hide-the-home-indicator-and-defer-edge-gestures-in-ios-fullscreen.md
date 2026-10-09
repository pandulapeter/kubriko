# Hide the home indicator and defer edge gestures in iOS fullscreen mode, and seed the status bar from the Kotlin state

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** iOS
**Challenged:** sound
**Files:** `app/ios/iosApp/iosApp/ContentView.swift`,
`app/ios/src/iosMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseViewController.kt`, `app/ios/CLAUDE.md`

Ships in no published artifact (`app/ios` is the Showcase).

## Problem

Fullscreen mode on iOS only hides the status bar:

```swift
struct ContentView: View {
    @State private var isStatusBarHidden = false

    var body: some View {
        ComposeView(isStatusBarHidden: $isStatusBarHidden)
            .ignoresSafeArea()
            .statusBarHidden(isStatusBarHidden)
    }
}
```

1. **Home indicator and edge gestures.** On Face ID iPhones and iPads the home indicator stays drawn over the bottom
   of the game, and a swipe from the bottom (home / app switcher) or top edge (Notification / Control Center) acts on
   the first touch. Games with controls near the edges (Space Squadron's movement, Wallbreaker's paddle) lose the app
   to a stray swipe. Android's fullscreen equivalent already uses `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, where the
   first edge swipe only reveals the bars.
2. **Status bar seeded from the wrong side.** The fullscreen flag lives in Kotlin as a process-level
   `private val isInFullscreenMode = mutableStateOf(false)` in `KubrikoShowcaseViewController.kt`, and Swift only
   learns about it from `onFullscreenModeChanged`, which fires on toggle:
   ```kotlin
   onFullscreenModeToggled = {
       isInFullscreenMode.value = !isInFullscreenMode.value
       onFullscreenModeChanged(isInFullscreenMode.value)
   },
   ```
   If SwiftUI rebuilds `ContentView` while the process lives on — the system discarding and reconnecting the
   `WindowGroup` scene after it was backgrounded — the new `@State` starts at `false` while Kotlin still says `true`:
   the Showcase is in fullscreen with the status bar showing.

## Fix

**Kotlin** (`KubrikoShowcaseViewController.kt`): add a top-level function next to `KubrikoShowcaseViewController`
that Swift reads the initial value from:

```kotlin
/** Whether the Showcase is in fullscreen mode, for the host to set up the system UI before the first toggle. */
fun isKubrikoShowcaseInFullscreenMode() = isInFullscreenMode.value
```

(Swift: `KubrikoShowcaseViewControllerKt.isKubrikoShowcaseInFullscreenMode()`.)

Recommended over the reviewer's "call `onFullscreenModeChanged(isInFullscreenMode.value)` in `viewDidLoad`":
`viewDidLoad` runs while SwiftUI is inside `makeUIViewController`, i.e. during a view update, and writing the
`@Binding` there triggers SwiftUI's "Modifying state during view update" warning and undefined ordering. Reading the
value when the `@State` is created avoids writing state at all.

**Swift** (`ContentView.swift`):

```swift
struct ContentView: View {
    @State private var isInFullscreenMode = KubrikoShowcaseViewControllerKt.isKubrikoShowcaseInFullscreenMode()

    var body: some View {
        ComposeView(isInFullscreenMode: $isInFullscreenMode)
            .ignoresSafeArea()
            .statusBarHidden(isInFullscreenMode)
            .modifier(FullscreenSystemOverlays(isInFullscreenMode: isInFullscreenMode))
    }
}

/// Hides the home indicator and makes the first swipe from a screen edge only reveal the system UI while the
/// Showcase is in fullscreen mode. Both modifiers need iOS 16; earlier versions keep the default behavior.
private struct FullscreenSystemOverlays: ViewModifier {
    let isInFullscreenMode: Bool

    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content
                .persistentSystemOverlays(isInFullscreenMode ? .hidden : .automatic)
                .defersSystemGestures(on: isInFullscreenMode ? .all : [])
        } else {
            content
        }
    }
}
```

Rename `ComposeView`'s `isStatusBarHidden` binding to `isInFullscreenMode` to match (it now drives more than the
status bar). The deployment target is iOS 15.3 (`IPHONEOS_DEPLOYMENT_TARGET = 15.3` in `project.pbxproj`), hence
the `#available` gate; do not raise it for this.

Update `app/ios/CLAUDE.md` → "Fullscreen handling": step 2 now also hides the home indicator and defers system edge
gestures (iOS 16+), and `ContentView` seeds its state from `isKubrikoShowcaseInFullscreenMode()` so a rebuilt SwiftUI
scene matches the process-level Kotlin flag. Keep the note that the SwiftUI hosting controller, not the embedded
controller, decides — it applies to `prefersHomeIndicatorAutoHidden` / `preferredScreenEdgesDeferringSystemGestures`
too, which is why these are SwiftUI modifiers and not overrides on the Kotlin `UIViewController`.

## Decision

Which edges to defer in fullscreen (`app/` is unpublished; a behaviour choice):
- **Recommended:** `.all` — matches Android's transient-bars behavior; every edge needs a second swipe.
- `.bottom` only — protects against the home gesture (the most frequent accident) but keeps Notification / Control
  Center one swipe away.

## Tests

None: SwiftUI and an iOS-only Kotlin entry point, no test source set in `app/ios`, and the Kotlin addition is a
one-line getter.

## Manual check

On a Face ID iPhone with iOS 16+ (and ideally an iPad):
1. Enter fullscreen from the Showcase → status bar and home indicator disappear (the indicator fades after a moment);
   a swipe up from the bottom edge first shows the indicator, a second swipe goes home; same for the top edge.
2. Leave fullscreen → status bar and indicator are back, single swipes act immediately.
3. Enter fullscreen, background the app, and let the system reclaim the scene (or simulate with Xcode's
   "Simulate Memory Warning" / reopening from the app switcher until the scene is rebuilt) → on return, the status
   bar is still hidden.
4. On an iOS 15 device or simulator → builds and runs; fullscreen hides only the status bar, as before.
