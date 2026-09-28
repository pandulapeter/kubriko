# Hide the iOS status bar in full screen through the SwiftUI root that actually controls it

**Challenged:** amended — step 3 also updates the two places in `app/ios/CLAUDE.md` that describe the factory as the parameterless `KubrikoShowcaseViewController()`, which the new signature makes wrong; the fix itself is unchanged.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** iOS
**Files:** `app/ios/src/iosMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseViewController.kt`,
`app/ios/iosApp/iosApp/ContentView.swift`, `app/ios/CLAUDE.md`

## Problem

The Kotlin view controller hides the status bar by overriding `prefersStatusBarHidden` and asking the key window's root
to re-query it (`KubrikoShowcaseViewController.kt`, ~line 25):

```kotlin
    override fun prefersStatusBarHidden() = isInFullscreenMode.value
    ...
            onFullscreenModeToggled = {
                isInFullscreenMode.value = !isInFullscreenMode.value
                UIApplication.sharedApplication.keyWindow?.rootViewController?.setNeedsStatusBarAppearanceUpdate()
            },
```

But the app's root is SwiftUI (`iOSApp.swift`: `WindowGroup { ContentView() }`), and the Kotlin controller is embedded
through a `UIViewControllerRepresentable` (`ContentView.swift`, `ComposeView`). UIKit asks only the root view controller
(the `UIHostingController` SwiftUI creates) and whatever it names through `childForStatusBarHidden`; the hosting
controller takes the answer from SwiftUI's `.statusBarHidden(_:)` preference and does not forward to representable
children. `Info.plist` does not set `UIViewControllerBasedStatusBarAppearance`, so the per-controller mechanism is on,
but the override above is never consulted: in full screen the top bar hides while the status bar (clock, battery) stays
over the game. Confidence is medium — this is the documented SwiftUI behaviour, not yet observed on a device for this
app.

Drop this plan if, on a device at the reviewed commit, the status bar already disappears when full screen is turned on.

## Fix

Let SwiftUI own the status bar, fed by the Kotlin state:

1. Kotlin: give the factory a callback and drop the UIKit status bar plumbing:

   ```kotlin
   fun KubrikoShowcaseViewController(onFullscreenModeChanged: (Boolean) -> Unit) = object : UIViewController(…) {
       …
       onFullscreenModeToggled = {
           isInFullscreenMode.value = !isInFullscreenMode.value
           onFullscreenModeChanged(isInFullscreenMode.value)
       },
   ```

   Remove the `prefersStatusBarHidden` override, the `setNeedsStatusBarAppearanceUpdate` call and the
   `UIApplication` import. Keep the file-level `isInFullscreenMode` state.
2. Swift (`ContentView.swift`): hold the state in `ContentView` and pass a binding into `ComposeView`:

   ```swift
   struct ComposeView: UIViewControllerRepresentable {
       @Binding var isStatusBarHidden: Bool

       func makeUIViewController(context: Context) -> UIViewController {
           let isStatusBarHidden = $isStatusBarHidden
           return KubrikoShowcaseViewControllerKt.KubrikoShowcaseViewController(
               onFullscreenModeChanged: { isFullscreen in isStatusBarHidden.wrappedValue = isFullscreen.boolValue }
           )
       }

       func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
   }

   struct ContentView: View {
       @State private var isStatusBarHidden = false

       var body: some View {
           ComposeView(isStatusBarHidden: $isStatusBarHidden)
               .ignoresSafeArea()
               .statusBarHidden(isStatusBarHidden)
       }
   }
   ```

   Check the exact Swift signature Kotlin/Native exports for the lambda parameter (it is normally
   `(KotlinBoolean) -> Void`, hence `.boolValue`) in the generated `ComposeApp` header and adapt.
   The `ContentView` above assumes plan 86 already removed the `windowSizeChanged` notifications and the size class
   environment values; if plan 86 was skipped, keep those `.onChange` modifiers and properties as they are and only add
   the state, the binding and `.statusBarHidden`.
3. Update `app/ios/CLAUDE.md` ("Fullscreen handling" point 2): the status bar is hidden by `ContentView`'s
   `.statusBarHidden`, driven by the `onFullscreenModeChanged` callback, because the SwiftUI hosting controller is the
   root and does not consult the embedded controller. The same file names the factory as `KubrikoShowcaseViewController()`
   twice (the intro sentence and the "Entry point" section); change both to
   `KubrikoShowcaseViewController(onFullscreenModeChanged)` and say that `ContentView.swift`'s `ComposeView` passes the
   callback that feeds its `@State`.

The iOS release workflow builds the Xcode project, so the Swift change ships with the next iOS build; nothing else calls
`KubrikoShowcaseViewController()`.

## Tests

None: this is UIKit/SwiftUI status bar behaviour, only observable on a simulator or device.

## Manual check

iPhone (or iPhone simulator) on iOS 17 or 18, run the `[iOS] Showcase` scheme from Xcode:

1. Open Wallbreaker, open its menu overlay and press the full screen button.
2. Expected: the top bar and the status bar (clock, battery) both disappear; pressing the button again brings both
   back.
3. Leave the game through its Leave button while in full screen: the status bar comes back with the top bar.
4. Rotate to landscape and back while in full screen: the status bar stays hidden.
