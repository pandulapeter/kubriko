# Drop the redundant on-line re-checks, the self-passing parameter and `else if (true)` from `PhysicsBody`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-physics
**Files:**
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/PhysicsBody.kt`
- `plugins/physics/src/commonMain/kotlin/com/pandulapeter/kubriko/physics/rays/Ray.kt`

## Problem
At 2480325f (line numbers before P05; re-locate by the quotes):
1. `density`'s setter (PhysicsBody.kt:116–123):
   ```kotlin
   if (density == 0f) {
       setStatic()
   } else if (true) {
       calculateMass(value)
   }
   ```
2. The polygon branch of `rayIntersect` (PhysicsBody.kt:283–297):
   ```kotlin
   val intersection = lineIntersect(startPoint, endPoint, startOfPolyEdge, endOfPolyEdge)
   if (intersection != null) {
       val distance = startPoint.distanceTo(intersection)
       if (isPointOnLine(startPoint, endPoint, intersection) && isPointOnLine(
               startOfPolyEdge,
               endOfPolyEdge,
               intersection
           ) && distance < maxD
       ) {
   ```
   `lineIntersect` (physics/implementation/Helpers.kt:17–45, `LineIntersection.kt` after P01) computes `x`, `y` and
   returns `null` unless `isPointOnLine(lineStart = line1Start, lineEnd = line1End, point = SceneOffset(x, y))` and
   `isPointOnLine(lineStart = line2Start, lineEnd = line2End, point = SceneOffset(x, y))` both hold, and otherwise returns
   that same `SceneOffset(x, y)`. With `line1 = (startPoint, endPoint)` and `line2 = (startOfPolyEdge, endOfPolyEdge)`
   the two re-checks are the identical calls on the identical values; `isPointOnLine` is a pure function of its float
   inputs, so both re-checks are always true when reached.
3. `internal fun rayIntersect(startPoint, endPoint, maxDistance, physicalBody: PhysicsBody)` (:231–236) only stores
   `physicalBody` as `closestBody` (:262, :296); its single caller passes the receiver itself:
   `body.rayIntersect(startPoint, endPoint, minT1, body)` (Ray.kt:83).

## Fix
1. `} else if (true) {` → `} else {`.
2. Reduce the condition to `if (distance < maxD) {` and remove the now-unused
   `import com.pandulapeter.kubriko.physics.implementation.isPointOnLine` from PhysicsBody.kt (`isPointOnLine` itself
   stays — `lineIntersect` uses it).
3. Remove the `physicalBody` parameter; assign `closestBody = this` in both branches; change Ray.kt's call to
   `body.rayIntersect(startPoint, endPoint, minT1)`.

## Behaviour
Unchanged: (1) `else if (true)` is `else`; (2) the removed checks are deterministic, side-effect-free calls whose
arguments are bit-identical to ones that already returned true inside `lineIntersect`; (3) `physicalBody === this` at
the only call site.

## Public API
None — `rayIntersect` is `internal`; the `density` setter's contract is unchanged.

## Tests
The existing ones (`RayIntersectionTest` covers circle and polygon hits, axis-aligned rays and misses).

## Verify
`./gradlew :plugins:physics:compileKotlinDesktop :plugins:physics:desktopTest`

## Manual check
none
