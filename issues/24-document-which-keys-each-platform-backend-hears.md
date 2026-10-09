# Document which key events each platform's keyboard backend hears

**Kind:** docs (behaviour decision)  ·  **Severity:** low  ·  **Platforms:** all
**Challenged:** sound
**Files:** `plugins/keyboard-input/CLAUDE.md`, `plugins/keyboard-input/README.md`, `plugins/keyboard-input/src/commonMain/kotlin/com/pandulapeter/kubriko/keyboardInput/KeyboardInputManager.kt` (class KDoc)

Ships in `io.github.pandulapeter.kubriko:plugin-keyboard-input` (KDoc only for the recommended option). **Decision**
on whether to unify the backends instead.

Run after plan 21 (iOS backend) and plan 23 (release pairing): the table below assumes both landed; adjust the iOS
row if plan 21 was dropped.

## Problem

The four backends hear different sets of key events, and nothing tells a game developer:

- **Desktop**: `Toolkit.addAWTEventListener(…, KEY_EVENT_MASK)` — every key event of every window of the JVM,
  including keys typed into a Compose `TextField` and keys Compose uses for focus traversal.
- **Web**: `window.addEventListener("keydown"/"keyup")` — every key event of the page, including those typed into
  Compose text fields.
- **Android**: `View.OnUnhandledKeyEventListener` on the decor view — only events **no view consumed**.
  `AndroidComposeView` consumes arrow keys and Tab when they move Compose focus, and a focused `clickable` consumes
  Enter/Space/D-pad center. A game with a focusable Compose control on screen (a pause button, a menu) loses those
  keys on Android only.
- **iOS** (after plan 21): `GCKeyboard` — every hardware key, regardless of Compose focus.

So a player typing a name into a `TextField` on Desktop/Web/iOS also steers the ship with WASD, while on Android a
focused button swallows the arrow keys the game listens to.

## Decision

- **(a) Document the difference (recommended).** No behaviour change. Games that mix text input and keyboard control
  gate their own handling (e.g. ignore keys while a text field is focused); games that want arrows on Android keep
  Compose focus off their in-game controls or set `focusProperties { canFocus = false }` on them.
- (b) Route every platform through a Compose key modifier (`onPreviewKeyEvent` via `processOverlayModifier`). Compose
  only dispatches key events to the focused node and its ancestors, and nothing inside the viewport is focusable, so
  the game would hear no keys unless the app parks focus inside the viewport — a breaking change for every game, and
  it would interfere with the gamepad plugin's focus navigation, which walks the same focus. Not recommended.
- (c) Android only: intercept in `Window.Callback.dispatchKeyEvent` to see keys before Compose. Requires replacing the
  Activity's window callback, which conflicts with the Activity and other libraries doing the same. Not recommended.

## Fix (for option a)

1. `CLAUDE.md`: add a "What each backend hears" paragraph above the Platform Differences table with the four bullets
   above (one line each), and a Gotcha: "Keys typed into a text field reach the game on Desktop, Web and iOS; Compose
   focus traversal and focused clickables can consume arrows, Tab, Enter and Space before the game sees them on
   Android."
2. `README.md`: a short "Platform notes" section with the same two sentences for game developers.
3. `KeyboardInputManager` class KDoc: one sentence — "Key events come from a platform listener, not from Compose's focus
   system: on Desktop, Web and iOS every key is heard, including keys typed into text fields; on Android only keys
   no view consumed (Compose focus navigation and focused clickables may consume arrows, Tab, Enter and Space)."

## Tests

None (documentation).

## Manual check

None beyond reading the rendered README.
