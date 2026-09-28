# Add round-trip tests for every type serializer the serialization plugin ships

**Challenged:** amended — `AngleDegreesSerializer`/`AngleRadiansSerializer` write `value.normalized`, so their contract is "comes back as the normalized value" (−0 and tiny angles do not survive bitwise, negatives and full turns wrap), not a raw round trip; `BoxBody` values respect its pivot clamp and its rotation goes through the same normalization; the unknown-key cases apply only to the seven structured serializers (the other four encode a bare number); the missing-element defaults are listed per serializer (`Scale`'s is `0f`, `BoxBody`'s scale `Scale.Unit`); the meaningless "no partial string" assertion is gone; `Color` is expected to be lossless (it stores the packed `ULong`).

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all (runs on the desktop JVM)  ·  **Artifact:** `plugin-serialization` (tests only)
**Files:** `plugins/serialization/src/commonTest/kotlin/com/pandulapeter/kubriko/serialization/typeSerializers/TypeSerializerRoundTripTest.kt` (new)

**Part of the testing extension.** It runs in lane C after `60` and `61`, whose `DeserializeActorsTest` and
`BoxBodySerializerTest` cover the manager's failure semantics and the missing-pivot default. It adds tests only.

## Problem

Save games and scene-editor files depend on eleven hand-written `KSerializer`s in `typeSerializers/`:
`AngleDegrees`, `AngleRadians`, `BoxBody`, `Color`, `Offset`, `PointBody`, `Scale`, `SceneOffset`, `SceneSize`,
`SceneUnit` and `Size`. Each writes its own descriptor and decode loop:

```kotlin
override fun deserialize(decoder: Decoder): SceneOffset {
    return decoder.decodeStructure(descriptor) {
        var x = 0f
        var y = 0f
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
```

A swapped index, a missing element or a lossy conversion (for example a `Color` through `Float` channels) shows up
only when a player's save loads wrong. Plan `61` found one such gap (the missing pivot). Only `BoxBody` gets a
round-trip test there, and the other ten have none.

## Fix

Add `TypeSerializerRoundTripTest` in `commonTest`. It is pure, needs no Kubriko instance, and each serializer is used
directly through `Json.encodeToString(Serializer, value)` / `Json.decodeFromString(Serializer, text)`.
- For `SceneUnit`, `Offset`, `SceneOffset`, `Size`, `SceneSize`, `Scale` and `PointBody`, a table of values:
  - `0`, `-0f` (assert the sign survives, via `toRawBits`; JSON writes `-0.0`), `1`, `-3.5`, `1e7`, `1e-7`, and a
    value with a non-trivial mantissa (`0.1f + 0.2f`).
  - For multi-field types, a value whose fields all differ, so a swapped index shows up.

  Each must round-trip bit for bit (`toRawBits` per component) through a default `Json`.
- `AngleDegrees` and `AngleRadians`: both serializers encode `value.normalized` (`(raw % turn + turn) % turn`) and
  decode it as the raw value. Assert, for the same table plus `-90`/`450` degrees and `-π/2`/`5π/2` radians, that the
  decoded `raw` equals `original.normalized` bit for bit. This pins the wrapping on purpose: `-0f` comes back as
  `+0f`, and in-range values lose low bits because `normalized` adds a full turn before the second `%` (checked in
  float32: `0.1f + 0.2f` radians comes back as `0.3000002`, `1e-7` as `0`, while `1f` survives). Do not `@Ignore` these; list the precision loss in the report
  as an observation, not a failing test.
- `Color`: opaque, fully transparent and half-transparent colors, plus `Color(0x12345678)`, must come back with the
  same `value` (`ULong`). The serializer stores the packed `ULong` as a `Long`, so this is expected to be exact; if it
  is not, that is a finding: `@Ignore` the case with the observed value and report it.
- `BoxBody` and `PointBody`: every property (position, size, pivot, scale, rotation) round-trips. `BoxBody`'s
  constructor clamps the pivot into `[0, size]`, so use positive sizes and pivots inside them (a value the
  constructor would change proves nothing about the serializer). Its rotation goes through `AngleRadiansSerializer`,
  so compare it with `rotation.normalized`. Do not duplicate `BoxBodySerializerTest`'s missing-pivot case.
- **Non-finite values** (float-based serializers only; `Color` has none):
  - With `Json { allowSpecialFloatingPointValues = true }`, NaN, `+∞` and `-∞` round-trip (NaN checked with
    `isNaN()` per component; `Offset(NaN, NaN)` is `Offset.Unspecified`, which is fine). For the angles, `normalized`
    turns `±∞` into NaN, so assert what `normalized` gives.
  - With a default `Json`, `encodeToString` throws a `SerializationException` (kotlinx throws its subclass
    `JsonEncodingException`; use `assertFailsWith<SerializationException>`).

  This pins the current behaviour: the plugin leaves the `Json` configuration to the actor's own `serialize()`, and
  `SerializationManagerImpl` uses its own `Json { ignoreUnknownKeys = true }` only for the wrapper list.
- **Unknown keys** (the seven structured serializers: `Offset`, `SceneOffset`, `Size`, `SceneSize`, `Scale`,
  `PointBody`, `BoxBody`; `SceneUnit`, the two angles and `Color` encode a bare number and have no keys): decoding a
  value's JSON with an extra key throws `SerializationException` with a default `Json` (the JSON decoder rejects the
  unknown name inside `decodeElementIndex`). With `ignoreUnknownKeys = true` it decodes the same value. Encode the
  JSON first and add the key through `JsonObject`, so the test does not depend on the exact element names.
- **Missing elements:** for each structured serializer, decode JSON with one element removed. None of the decode loops
  checks for missing elements, so assert the default each loop starts from: `0f` for every field of `Offset`,
  `SceneOffset`, `Size`, `SceneSize` and `Scale` (so a `Scale` without `vertical` decodes as `0f`, not `1f`);
  `SceneOffset.Zero` for `PointBody`; for `BoxBody` `SceneOffset.Zero` position, `SceneSize.Zero` size, `Scale.Unit`
  scale and `AngleRadians.Zero` rotation, with the pivot then clamped into the decoded size (`61` owns the missing-pivot
  default). Record whichever the code does; the purpose is that a later change to it is deliberate.

## Tests

This plan is the tests. Run `./gradlew :plugins:serialization:desktopTest`, then `./gradlew :plugins:serialization:build`.
A failure that shows the serializer loses data is a finding: `@Ignore` it with the observed value and report it. It
is not fixed under this plan.

## Manual check

None.
