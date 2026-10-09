# Read iOS hardware keyboard input through GCKeyboard instead of a first-responder view

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** iOS (hardware keyboards: iPad Magic/Smart Keyboard, Bluetooth, the simulator's Mac keyboard)
**Challenged:** amended — the shared runtime dispatches over a copy-on-write listener array (a listener whose callback disposes a Kubriko instance would otherwise modify the list mid-dispatch), confines register/unregister and the notification observers to the main thread, and documents that it owns the process's single `keyChangedHandler`.
**Files:** `plugins/keyboard-input/src/iosMain/kotlin/com/pandulapeter/kubriko/keyboardInput/implementation/KeyboardEventHandler.ios.kt`, `plugins/keyboard-input/CLAUDE.md`, `plugins/keyboard-input/README.md` (only if it gains a platform section; today it has none)

Ships in `io.github.pandulapeter.kubriko:plugin-keyboard-input`. No public API change. Observable change: iOS games
start receiving hardware key events they (very likely) never received; nothing that worked stops working.

## Problem

The iOS backend creates a zero-size `UIView` that overrides `pressesBegan`/`pressesEnded`/`pressesCancelled` and
tries to make it first responder:

```kotlin
// KeyboardEventHandler.ios.kt
init {
    inputView.becomeFirstResponder()
    val currentViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
    currentViewController?.view?.addSubview(inputView)
}
```

- `becomeFirstResponder()` is called **before** the view is in a window, so UIKit refuses it (returns false).
- The view is then added as a leaf subview of the root view controller's view. `UIPress` events travel from the first
  responder **up** its `nextResponder` chain (superviews → view controller → window → application); they never reach
  a sibling leaf. Compose's own input view (`InputViews.ios.kt`, `canBecomeFirstResponder() = true`) is the one
  Compose makes first responder when the window has none (`ComposeSceneMediator.ios.kt`: `if
  (findFirstResponder(window) == null) _overlayView.becomeFirstResponder()`), so the presses go there and up, never
  into Kubriko's view.
- `UIApplication.keyWindow` is deprecated and is `nil` before the window becomes key (the first composition can run
  before that), in which case the view is not attached at all.
- Even if it did become first responder (the "minimal fix": attach first, `becomeFirstResponder()` on the next main
  loop pass, find the window through `connectedScenes`), it would **steal** hardware key events from Compose —
  Compose's focus traversal and `onKeyEvent` handlers stop working — and lose the role for good the first time a
  Compose `TextField` takes first responder, since Compose hands it back to its own view, not Kubriko's.

`plugins/keyboard-input/CLAUDE.md` also misdescribes the platform: "Zero-size UIView first-responder | Limited key
support; software keyboard only". `UIPress` (and the `GCKeyboard` below) are hardware-keyboard only; the software
keyboard produces text input, not key events, on every route.

Confidence: high that no key events arrive today (the chain analysis above, and Compose's sources at 1.12.1);
confirm on a device as part of the manual check before changing anything.

## Fix

Options considered:

- **(a) `GCKeyboard` from the GameController framework (recommended).** iOS 14+ (the Showcase targets 15.3).
  `GCKeyboard.coalescedKeyboard?.keyboardInput?.keyChangedHandler` delivers `(GCKeyboardInput?,
  GCControllerButtonInput?, GCKeyCode, Boolean pressed)` for every physical key, independent of the responder chain
  and of Compose's focus, so it behaves like the Desktop (AWT global listener) and Web (window listener) backends.
  It does not consume the events: Compose's own key handling keeps working. `GCKeyCode` values are HID usage codes,
  the same numbers Compose iOS uses for `Key` (`Key.ios.kt`: `Key(UIKeyboardHIDUsage…)`), so `Key(keyCode)` maps
  directly, exactly as the current `UIPress.toKey()` does. The `gamepad-input` iOS backend already uses
  GameController, so the framework is no new dependency.
- (b) Compose key modifier (`Modifier.onPreviewKeyEvent` through `processOverlayModifier`). Compose dispatches key
  events only to the focused node and its ancestors (`FocusOwnerImpl.dispatchKeyEvent`), and nothing in the viewport
  is focusable, so the game would only hear keys while the app has placed focus inside the viewport — different from
  every other platform. Rejected.
- (c) The minimal UIKit fix. Rejected for stealing Compose's key events and losing them to text fields (above).

Implementation of (a):

1. Replace the `UIView` with a process-wide `private object KeyboardRuntime` in the iOS file. `keyChangedHandler` is
   a single property, so several `Kubriko` instances (or two `KeyboardInputManager`s) must share one handler that fans
   out: keep the listeners (pairs `(onKeyPressed, onKeyReleased)`) in a copy-on-write array — `register`/`unregister`
   replace the array, the handler iterates whatever array it read at the start of the event — so a callback that
   disposes a Kubriko instance (and so unregisters) mid-dispatch cannot break the iteration, and dispatching a key
   allocates nothing; `register`/`unregister` from `startListening`/`stopListening`. Both run on the main thread
   (`startListening` is called from composition; `stopListening` can come from `kubriko.dispose()` on any thread, so
   when `NSThread.isMainThread` is false it hops with `dispatch_async(dispatch_get_main_queue())`), which keeps the
   array, the held-key set and the handler installation single-threaded; install the handler on `GCKeyboard.coalescedKeyboard` when the first listener
   registers, and observe (with `queue = NSOperationQueue.mainQueue`) `GCKeyboardDidConnectNotification` (install on
   the new coalesced keyboard — `coalescedKeyboard` can still be `nil` at launch with a keyboard attached, the
   notification follows) and
   `GCKeyboardDidDisconnectNotification` (report every key still held as released — no `pressed = false` arrives for
   them). Remove the observers and the handler when the last listener unregisters.
2. The handler runs on the main queue by default (`GCController`/`GCKeyboard` handler queue), which is the thread the
   manager expects key events on (the other platforms deliver them there too); keep it that way, do not set a
   custom `handlerQueue`.
3. Dispatch `Key(keyCode)` to `onKeyPressed` when `pressed`, else `onKeyReleased`. The manager already ignores
   repeated presses of a held key.
4. Move the work out of the factory's `init` into `startListening()` (today `startListening()` is `Unit` and the view
   is attached during composition).
5. `CLAUDE.md` Platform Differences row for iOS: Backend `GameController GCKeyboard (process-wide handler shared by all
   instances; it replaces any keyChangedHandler the app itself installs on the coalesced keyboard)`; Gotcha: hardware keyboards only (the software keyboard sends no key events); keys are heard whatever
   Compose has focused, like Desktop and Web. Drop "software keyboard only".

Drop this plan if the manual check shows the current backend does receive key events on a device (then only fix the
`CLAUDE.md` row).

## Tests

None: GameController is not available on the JVM test classpath, and the mapping is the identity on `GCKeyCode`.

## Manual check

iPad with a hardware keyboard (or the iOS simulator with I/O → Keyboard → Connect Hardware Keyboard), Showcase app:
1. Before the change, open `test-input` (with `showcase.areTestExamplesEnabled=true`) or Wallbreaker and confirm key
   presses are not reported.
2. After: arrow keys/WASD move, Space/Enter act, holding a key reports it every tick, releasing it reports a release.
3. Focus any Compose `TextField` the app shows and type: the text field still receives the text; Tab/arrow focus traversal in Compose UI still works.
4. Disconnect the keyboard while holding a key: the game must see the release.
