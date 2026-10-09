# Turn `KeyboardInputManagerImpl`'s latch comment into KDoc and drop "as before"

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-keyboard-input
**Files:**
- `plugins/keyboard-input/src/commonMain/kotlin/com/pandulapeter/kubriko/keyboardInput/KeyboardInputManagerImpl.kt`

## Problem
At 2480325f:
- `keysPressedSinceLastSnapshot` (:41) is documented by a `//` block (:37–40) instead of KDoc.
- `onUpdate` (:78–80) carries fix history: "…otherwise the dirty flag clears **as before**."

## Fix
- Turn :37–40 into KDoc on `keysPressedSinceLastSnapshot`, same content.
- :78–80 → "// When the snapshot exists only to surface latch-only keys (already released), force a rebuild next tick
  so they are dropped once they have been observed for one tick."

## Behaviour
Unchanged — comments only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:keyboard-input:compileKotlinDesktop`

## Manual check
none
