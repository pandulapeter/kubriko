# Document plugin-serialization's type serializers and their typealiases

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-serialization
**Files:**
- `plugins/serialization/src/commonMain/kotlin/com/pandulapeter/kubriko/serialization/typeSerializers/AngleDegreesSerializer.kt`
- `.../typeSerializers/AngleRadiansSerializer.kt`
- `.../typeSerializers/BoxBodySerializer.kt`
- `.../typeSerializers/ColorSerializer.kt`
- `.../typeSerializers/OffsetSerializer.kt`
- `.../typeSerializers/PointBodySerializer.kt`
- `.../typeSerializers/ScaleSerializer.kt`
- `.../typeSerializers/SceneOffsetSerializer.kt`
- `.../typeSerializers/SceneSizeSerializer.kt`
- `.../typeSerializers/SceneUnitSerializer.kt`
- `.../typeSerializers/SizeSerializer.kt`

(all under `plugins/serialization/src/commonMain/kotlin/com/pandulapeter/kubriko/serialization/`)

## Problem
Each of the 11 files declares a public `typealias Serializable<Type> = @Serializable(with = <Type>Serializer::class)
<Type>` and a public `object <Type>Serializer : KSerializer<Type>`, none with KDoc (e.g. BoxBodySerializer.kt:30 and
:35, ColorSerializer.kt:23 and :28). The descriptor names are part of the wire format a saved scene depends on:
`angleDegrees`, `angleRadians` (float), `color` (long), `sceneUnit` (float), and class descriptors `rectangleBody`
(for `BoxBody`), `offset`, `pointBody`, `scale`, `sceneOffset`, `sceneSize`, `size`.

## Fix
One short KDoc on each typealias (use it as a property type in a `@Serializable` state class so the value is
encoded with the matching serializer) and on each object (what it encodes, how — primitive kind or the element
names — and its descriptor name; for `BoxBodySerializer` say explicitly that the descriptor is `"rectangleBody"`, kept
for compatibility with saved scenes). Read each object's `serialize` to name its elements. Nothing else changes.

This plan touches 11 files; it runs last in the lane.

## Behaviour
Unchanged — documentation only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:serialization:compileKotlinDesktop`

## Manual check
none
