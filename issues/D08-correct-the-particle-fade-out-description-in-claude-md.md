# Correct the description of the particle fade-out tail in demo-particles' `CLAUDE.md`.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-particles/CLAUDE.md`

## Problem
`examples/demo-particles/CLAUDE.md:43-44` says "`draw()` renders a filled HSV circle for the first 70% of lifetime, then a plain black-outline ghost for the fade-out tail". The code (`examples/demo-particles/src/commonMain/kotlin/com/pandulapeter/kubriko/demoParticles/implementation/actors/DemoParticleState.kt:94-99`) draws `drawCircle(color = Color.Black.copy(alpha = 1f - currentProgress), radius = radius, center = body.size.center.raw)` with no `style`, i.e. a filled black circle with fading alpha. (The first 70% is a filled HSV circle *with* a black stroke outline.)

## Fix
Replace "then a plain black-outline ghost for the fade-out tail" with "then a fading filled black circle for the tail", and mention the black outline on the HSV phase if it fits the sentence.

## Behaviour
Docs only.

## Public API
None.

## Tests
None.

## Verify
none (docs only)

## Manual check
none
