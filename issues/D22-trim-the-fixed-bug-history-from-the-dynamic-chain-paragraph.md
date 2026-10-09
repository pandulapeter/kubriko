# Trim the fixed-bug history from demo-physics `CLAUDE.md`'s `DynamicChain` paragraph.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-physics/CLAUDE.md`

## Problem
The `**`DynamicChain`**` paragraph contains a postmortem: "Reading them made the clip/cull bounds lag the drawn path by one tick — invisible at 60 FPS, but enough to clip the chain visibly at low/throttled frame rates." The code-style skill keeps only the constraint, not the history.

## Fix
Delete that sentence. Keep the constraint ("reads `ChainLink.physicsBody.position` … **not** `ChainLink.body.position`: as a `Group` parent this actor updates before its child links in the same tick …, so the links' render bodies still hold the previous tick's position") and the closing "`physicsBody.position` is the same value the links copy into their render bodies this tick, so bounds and path stay in sync." Do not touch the "`linkCount` `ChainLink` … `linkCount - 1` joints" wording — planned D50 fixes it together with the code.

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
