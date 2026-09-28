# Anchor the iOS share sheet's popover and present it from the active scene's top-most controller

**Challenged:** sound

**Kind:** bug (crash)  ·  **Severity:** high  ·  **Platforms:** iOS (iPad crash; iPhone robustness)
**Artifact:** `tool-ui-components`
**Files:** `tools/ui-components/src/iosMain/kotlin/com/pandulapeter/kubriko/uiComponents/utilities/ShareManager.ios.kt`, `tools/ui-components/CLAUDE.md`

## Problem

```kotlin
override fun shareText(text: String) {
    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
        UIActivityViewController(listOf(text), null),
        animated = true,
        completion = null,
    )
}
```

1. On iPad, `UIActivityViewController` is presented as a popover and UIKit raises `NSGenericException` ("…must have a non-nil sourceView or barButtonItem…") when neither `sourceView`/`sourceRect` nor `barButtonItem` is set — the app crashes. The Showcase's iOS target is built for iPad: `TARGETED_DEVICE_FAMILY = "1,2"` in both target configurations of `app/ios/iosApp/iosApp.xcodeproj/project.pbxproj`, and About → Share (`AboutScreen.kt` ~108, `rememberShareManager()`) calls this.
2. The app is scene-based (`UIApplicationSceneManifest` in `app/ios/iosApp/iosApp/Info.plist`). `UIApplication.keyWindow` is deprecated since iOS 13 and can be `nil` there (e.g. during a scene transition), in which case sharing silently does nothing.
3. Presenting on the root controller while it already presents something (an alert, another sheet) fails with "Attempt to present … which is already presenting" and nothing appears.

## Fix

In `ShareManager.ios.kt`:
1. Find the window: iterate `UIApplication.sharedApplication.connectedScenes` for a `UIWindowScene` whose `activationState == UISceneActivationStateForegroundActive`, take its window with `isKeyWindow()` or else its first window. Fall back to `keyWindow` only if no scene matches. (The deployment target is 15.3, so the scene APIs are always available.)
2. Walk to the top-most controller: `var presenter = window.rootViewController; while (presenter?.presentedViewController != null) presenter = presenter.presentedViewController`. Return if it is `null`.
3. Before presenting, anchor the popover: `controller.popoverPresentationController?.let { popover -> popover.sourceView = presenter.view; popover.sourceRect = presenter.view.bounds.useContents { CGRectMake(origin.x + size.width / 2, origin.y + size.height / 2, 0.0, 0.0) }; popover.permittedArrowDirections = 0u }` — centered, no arrow, since the shared API has no anchor view. On iPhone `popoverPresentationController` is `null`/unused and nothing changes.
4. `tools/ui-components/CLAUDE.md` → the `ShareManager` bullet: append "On iOS the sheet is presented from the active scene's top-most view controller, as a centered popover on iPad."

No public signature changes (`ShareManager`, `rememberShareManager` stay as they are).

## Tests

None: the code only calls UIKit, which cannot run in `desktopTest` or in a unit test without a simulator host app.

## Manual check

Build and run the Showcase from Xcode:
1. iPad simulator (any iPad): About → Share → the share sheet appears as a centered popover; dismiss it; repeat twice. Before the fix the app crashes on the first tap.
2. iPhone simulator: About → Share → the sheet slides up as before.
3. iPad, Split View / Stage Manager with the Showcase in one pane: Share still appears in the Showcase's own window.
