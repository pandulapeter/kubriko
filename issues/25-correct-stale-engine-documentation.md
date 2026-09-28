# Correct stale engine documentation: frame-rate default, paused Managers, bounding boxes, ActorManager defaults

**Challenged:** sound

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Artifact:** `engine` (KDoc only)
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/types/TargetFrameRate.kt` (KDoc), `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/StateManager.kt` (KDoc), `CLAUDE.md` (ActorManager and ViewportManager sections), `engine/CLAUDE.md` (Key Internal Files, Gotchas)

Apply last in the lane: plans 02, 03, 05, 07, 08, 13, 18, 19 edit neighbouring paragraphs of the same `CLAUDE.md` files; this plan only touches the sentences quoted below.

## Problem

Each statement below is false at 0008d027:

1. `TargetFrameRate.DisplayDefault` KDoc: "Updates occur on every display frame, so the game loop runs at the device's maximum refresh rate. **This is the default.**" — `ViewportManager.newInstance` defaults to `initialTargetFrameRate: TargetFrameRate = TargetFrameRate.Limit(60)`, and its own KDoc explains why ("so a 120 Hz+ panel doesn't silently pay double the update/render cost").
2. Root `CLAUDE.md` → `ViewportManager` code block: `initialTargetFrameRate: TargetFrameRate = TargetFrameRate.DisplayDefault,` and the bullet "`TargetFrameRate.DisplayDefault` (default) ticks every display frame".
3. `StateManager.isRunning` KDoc: "When false, actors and managers do not receive updates." — `KubrikoImpl.onTick` calls every Manager's `onUpdate` regardless (plugins such as `PhysicsManagerImpl`/`ParticleManagerImpl` check `isRunning` themselves), and `Dynamic` actors keep updating when `shouldUpdateActorsWhileNotRunning = true`.
4. `engine/CLAUDE.md` → *Key Internal Files*: "`AxisAlignedBoundingBox.kt` — packs four 16-bit quantized coords into one `Long`. `QUANT_SHIFT = 4` means 16-unit precision; caps usable scene coords at ~±524 k scene units" and *Gotchas*: "`AxisAlignedBoundingBox` quantizes to multiples of 16 — positions between steps appear at the next multiple in culling (conservative, intentional)". The class now holds four `Float`s (`minXRaw`, `minYRaw`, `maxXRaw`, `maxYRaw`) mutated in place by its owner, with full `Float` range and precision.
5. Root `CLAUDE.md` → `ActorManager` code block: `invisibleActorMinimumRefreshTimeInMillis: Long = 0,` — the default is `100`; the block also omits `farAwayActorSleepMargin: SceneUnit? = null` and `shouldComposeLayers: Boolean = true`.

## Fix

1. `TargetFrameRate.DisplayDefault` KDoc: replace "This is the default." with "Pass it to `ViewportManager.newInstance` or `setTargetFrameRate` for uncapped updates; the default is `Limit(60)`."
2. Root `CLAUDE.md`: the code block line becomes `initialTargetFrameRate: TargetFrameRate = TargetFrameRate.Limit(60),`; in the bullet, move "(default)" from `DisplayDefault` to `Limit(fps)` ("`TargetFrameRate.Limit(fps)` (default `Limit(60)`) caps …; `TargetFrameRate.DisplayDefault` ticks every display frame (device maximum)").
3. `StateManager.isRunning` KDoc: "When false, [Dynamic] actors stop receiving updates (unless `shouldUpdateActorsWhileNotRunning` is set on the [ActorManager]); Managers keep receiving `onUpdate` and should check this flag themselves where pausing matters." (Import/KDoc-link `Dynamic` and `ActorManager` as needed.)
4. `engine/CLAUDE.md`: the *Key Internal Files* bullet becomes "`AxisAlignedBoundingBox.kt` — four `Float` bounds, mutated in place by the owning body or collision mask, so per-frame updates allocate nothing; full `Float` range and precision." Delete the quantization bullet from *Gotchas*.
5. Root `CLAUDE.md` `ActorManager` code block: `invisibleActorMinimumRefreshTimeInMillis: Long = 100,` and add `farAwayActorSleepMargin: SceneUnit? = null,` and `shouldComposeLayers: Boolean = true,` in declaration order (matching `ActorManager.newInstance`).

## Tests

None (documentation only). Verify each corrected statement against the code it describes before committing.

## Manual check

None.
