# Turn `PointerInputManagerImpl`'s declaration comments into KDoc

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-pointer-input
**Files:**
- `plugins/pointer-input/src/commonMain/kotlin/com/pandulapeter/kubriko/pointerInput/PointerInputManagerImpl.kt`

## Problem
The code-style skill: a comment that documents a declaration is KDoc, never `//`. At 2480325f these declarations carry
`//` blocks: `pendingPositionUpdates` (:61–63), `pointersPressedSinceLastTick` (:91–94), `pointersPendingCancellation`
(:96–100), `PointerInputChange.isCancellation` (:159–162), `ResolvedPointerPositions` (:177–180) and
`pointerInputHandling` (:254–255).

## Fix
Run after P16. Convert each of those six blocks into a KDoc block on the same declaration, same content, re-wrapped to
the file's width. Leave `//` notes on statements inside function bodies alone.

## Behaviour
Unchanged — comments only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:pointer-input:compileKotlinDesktop`

## Manual check
none
