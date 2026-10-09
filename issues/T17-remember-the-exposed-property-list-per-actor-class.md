# Discover the selected actor's @Exposed properties once per class instead of on every recomposition

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/InstanceManagerColumn.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/PropertyEditorMapper.kt, tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/ExposedPropertiesTest.kt (new), tools/scene-editor/CLAUDE.md

## Problem
`InstanceManagerColumn.kt:97-108` runs Kotlin reflection inside the `LazyColumn` content on every recomposition:
```kotlin
                    selectedInstance::class.memberProperties
                        .filterIsInstance<KMutableProperty<*>>()
                        .sortedBy { it.name }
                        .mapNotNull { property -> property.toPropertyEditor(...) }
```
and `toPropertyEditor` calls `setter.findAnnotation<Exposed>()` per property. The column recomposes on every drag frame
(`notifySelectedActorUpdate` flips the `selectedUpdatableActor` toggle), so the filter/sort/annotation lookup reruns many
times a second while dragging. `tools/scene-editor/CLAUDE.md` (Property inspector) claims "discovery only happens on selection
change", which is not true.

Verified behaviour-preserving: which properties are listed, and in which order, depends only on the class
(`memberProperties`, the setter's `@Exposed` annotation, the name sort). The editors themselves read the current value through
`getter.call(actor)` inside their Composable lambdas and take the callbacks/modes as arguments, so they are still rebuilt
from the remembered list every recomposition exactly as now.

## Fix
- In `PropertyEditorMapper.kt` add
  `internal fun exposedMutableProperties(type: KClass<*>): List<KMutableProperty<*>> = type.memberProperties.filterIsInstance<KMutableProperty<*>>().filter { it.setter.findAnnotation<Exposed>() != null }.sortedBy { it.name }`
  (a stable sort of the filtered list yields the same order as sorting first and dropping non-exposed ones afterwards).
  Leave `toPropertyEditor` unchanged (it still sets `isAccessible` and reads `@Exposed.name`).
- In `InstanceManagerColumn`, inside the `Column { ... }` and before the `LazyColumn` (the LazyColumn content lambda is not
  composable, so `remember` cannot go there):
  `val exposedProperties = remember(selectedInstance?.let { it::class }) { selectedInstance?.let { exposedMutableProperties(it::class) }.orEmpty() }`
  and in the `else` branch start the chain from `exposedProperties.mapNotNull { property -> property.toPropertyEditor(...) }`.
  Drop the now-unused imports (`memberProperties` and, if unused, `KMutableProperty`).
- `tools/scene-editor/CLAUDE.md` → Property inspector: replace "(no allocation per-frame — discovery only happens on selection
  change)" with "the exposed properties are discovered once per actor class (`exposedMutableProperties`, remembered by
  `InstanceManagerColumn`); the editor lambdas are rebuilt from that list on recomposition".

## Behaviour
Same editors, same order, same values; the reflection runs once per selected class instead of per recomposition.

## Public API
None (internal).

## Tests
`ExposedPropertiesTest` in package `...userInterface.panels.instanceManagerColumn` with a private fixture class (no need to
implement `Editable`; the function takes a `KClass<*>`):
```kotlin
private class Fixture {
    var zeta: Int = 0
        @Exposed(name = "Zeta") set
    var alpha: Float = 0f
        @Exposed(name = "Alpha") set
    var notExposed: Int = 0
    val readOnly: Int = 1
}
```
Assert `exposedMutableProperties(Fixture::class).map { it.name } == listOf("alpha", "zeta")`.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop :tools:scene-editor:desktopTest`

## Manual check
Select an actor with exposed properties in the Scene Editor (e.g. `demo-performance`): the property panel lists the same fields in
the same order; drag the actor and edit a field — values update live.
