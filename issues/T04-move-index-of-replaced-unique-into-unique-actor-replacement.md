# Move indexOfReplacedUnique out of UndoRedoHistory.kt into UniqueActorReplacement.kt, with its test

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/UndoRedoHistory.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/UniqueActorReplacement.kt (new), tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/UndoRedoHistoryTest.kt, tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/UniqueActorReplacementTest.kt (new)

## Problem
`helpers/UndoRedoHistory.kt` ends with a function that has nothing to do with undo/redo — it mirrors the engine's
`Unique` replacement rule for the controller's scene tracking:
```kotlin
internal fun indexOfReplacedUnique(
    actorClasses: List<KClass<*>>,
    newClass: KClass<*>,
    isUnique: Boolean,
) = if (isUnique) actorClasses.indexOf(newClass) else -1
```
Its test `onlyUniqueActorsOfTheSameClassAreReplaced` lives in `UndoRedoHistoryTest` and uses the private classes `A`, `B`, `C` there.

## Fix
- Move `indexOfReplacedUnique` with its KDoc verbatim into `helpers/UniqueActorReplacement.kt` (same package, MPL-2.0 header,
  `import kotlin.reflect.KClass`). Remove the now-unused `KClass` import from `UndoRedoHistory.kt`. `restoredSelectionIndex`
  stays in `UndoRedoHistory.kt` (it is about restoring selection after undo).
- Move the test method verbatim into a new `UniqueActorReplacementTest` in `tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/UniqueActorReplacementTest.kt`
  with its own private `A`, `B`, `C` classes. In `UndoRedoHistoryTest` delete the test and the private classes `A`, `B`, `C`
  if nothing else uses them (at 2480325f nothing does) and any import left unused.
- Grep the repo for `indexOfReplacedUnique` in docs (none expected).

## Behaviour
Verbatim move.

## Public API
None (internal).

## Tests
The moved test; the existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop :tools:scene-editor:desktopTest`

## Manual check
None.
