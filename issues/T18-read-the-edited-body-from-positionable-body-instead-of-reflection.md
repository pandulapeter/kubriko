# Read the body in BodyPropertyEditor from Positionable.body instead of the first PointBody-typed member found by reflection

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/propertyEditors/BodyPropertyEditor.kt

## Problem
`BodyPropertyEditor.kt:62-64`:
```kotlin
    val actor = getActor()
    val bd = actor::class.memberProperties.firstOrNull { it.returnType.isSubtypeOf(PointBody::class.createType()) }
    val body = bd!!.getter.call(actor) as PointBody
```
`getActor` already returns a `Positionable` (every `Editable` is one), whose `body: PointBody` is the body the editor drags,
picks and highlights. The reflection picks the *first* member property whose type is a `PointBody` subtype, in an order
kotlin-reflect does not specify — an `Editable` with a second body-typed property (a hitbox, a cached copy, a private
`BoxBody`) can get the wrong one edited. It also runs reflection, `createType()` and `isSubtypeOf` on every recomposition of the panel.

Verified for every in-repo `Editable` (demo-performance `MovingBox`, `Camera`, `BoxWithCircle`; game-blockys-journey `Blocky`,
`Block`; game-annoyed-penguins `Ground`, `Slingshot`, `Star`, `DestructiblePhysicsObject` subclasses; demo-physics `DynamicBox`,
`DynamicCircle`, `DynamicChain`, `StaticBox`, `StaticCircle`, `StaticPolygon`): the only `PointBody`-typed member is
`override val body`, so `getActor().body` returns the very object the reflection found. Tesselar has no `Editable`.

## Fix
Replace the three lines with `val body = getActor().body` and drop the imports `PointBody` (if unused), `createType`,
`isSubtypeOf`, `memberProperties`.

## Behaviour
Unchanged for every in-repo actor. For an actor with another body-typed property, the panel now edits the actor's own
`Positionable.body` — the one the rest of the editor uses.

## Public API
None.

## Tests
None possible as a unit test (Composable). The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
In the Scene Editor select a `BoxBody` actor and a `PointBody` actor (`Camera` in demo-performance): Position (and for the
box Pivot/Size/Scale/Rotation) edit the actor as before.
