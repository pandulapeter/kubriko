<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Compare and group Events by one EventTrigger that ignores the fields their type doesn't use

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** ACT-15  ·  **Depends on:** G05
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/EventDefinition.kt`, `actors/EventTrigger.kt` (new), `actors/ActorTypeFamily.kt`, `actors/EventFunctions.kt` (from G05), `actors/ActorTypeFeatures.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors/ActorTypeFamilyTest.kt`

## Problem

`EventDefinition` has three nullable fields, each meaningful for one `type` only:

```kotlin
internal data class EventDefinition(
    @SerialName("type") val type: EventType,
    @SerialName("key") val key: KeyChoice? = null,
    @SerialName("otherActorType") val otherActorType: String? = null,
    @Serializable(with = TimerNameSerializer::class)
    @SerialName("timer") val timer: String? = null,
    @SerialName("blocks") val blocks: List<BlockDefinition> = emptyList(),
)
```

Commands reject a stray field (`ActorTypeValidation.validateEvent`: `event.key != null && event.type != EventType.KEY_PRESSED -> Rejection(...)`, and the same for the other two), but `BlockTypeStore.load` accepts a hand-edited file with one. `ActorTypeFamily.replaces` compares all four:

```kotlin
fun EventDefinition.replaces(other: EventDefinition) = type == other.type && key == other.key && otherActorType == other.otherActorType && timer == other.timer
```

while the family generator's hooks (`Hooks.of`, `EventFunctions.of` after G05) look only at the field of the Event's type. So a child's `everyFrame` Event with a stray `"timer": "x"` overrides its parent's `onEveryFrame` hook in the generated code (the parent's Blocks no longer run for the child), while `ActorTypeFamily.eventsOf` keeps the parent's Event as inherited and the editor (`ui/actorTypes/Inheritance.kt`) shows it greyed as still running, without the button to replace it. What the user sees disagrees with what the game does, and `ActorTypeFeatures` (built from `eventsOf`) counts an Event the code never runs.

## Fix

- `actors/EventTrigger.kt`:
  ```kotlin
  /**
   * What makes an Event happen: its type and the one detail that type has. Two Events with the same trigger run at the same moment, and a child's replaces its parent's.
   */
  internal data class EventTrigger(val type: EventType, val key: KeyChoice?, val otherActorType: String?, val timer: String?)
  ```
- In `EventDefinition`: `val trigger get() = EventTrigger(type, key.takeIf { type == EventType.KEY_PRESSED }, otherActorType.takeIf { type == EventType.COLLISION }, timer.takeIf { type == EventType.TIMER_FINISHED })`. A getter isn't serialized, so the format doesn't change (G01's shape test confirms).
- `replaces` becomes `trigger == other.trigger`. Group by `trigger` wherever generators group Events today (the key, collision-target, and Timer groupings in `EventFunctions` and `ActorTypeFeatures`), so the hook an Event lands in and whether it replaces its parent's are decided by the same value.
- Not an identity: a type may have several Events with one trigger (they run in order), so the editor's Event selection (lane H, H07) must not key on it.
- Deferred, not part of this plan: a sealed Event model (a JSON format change, needing G01's raise).

## Tests

- `ActorTypeFamilyTest.aStrayFieldDoesNotStopAnEventReplacingItsParents`: parent `Enemy` with `EVERY_FRAME` `[Stop]`, child `Bat` with `EventDefinition(EventType.EVERY_FRAME, timer = "x", blocks = listOf(RemoveSelf))`: `family.eventsOf("Bat")` is only the child's Event. Fails before.
- `ActorTypeFamilyTest.eventsOfDifferentKeysStillDoNotReplaceEachOther`: `KEY_PRESSED` `SPACE` doesn't replace `KEY_PRESSED` `LEFT`.
