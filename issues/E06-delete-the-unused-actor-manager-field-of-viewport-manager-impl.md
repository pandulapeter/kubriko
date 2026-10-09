# Delete the never-read actorManager field of ViewportManagerImpl

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** engine
**Files:**
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ViewportManagerImpl.kt`

## Problem

```kotlin
private lateinit var actorManager: ActorManagerImpl               // :43
...
override fun onInitialize(kubriko: Kubriko) {                     // :96-98
    actorManager = (kubriko as KubrikoImpl).actorManager
}
```

The field is assigned and never read (`grep -n actorManager` in the file finds only these two lines); `onInitialize`
does nothing else.

## Fix

Delete the field, the whole `onInitialize` override (the base `Manager.onInitialize` is a no-op `= Unit`), and the
two imports that only they used: `com.pandulapeter.kubriko.Kubriko` and `com.pandulapeter.kubriko.KubrikoImpl`
(re-grep the file to confirm neither name remains).

## Behaviour
Unchanged. The removed cast cannot fail (`Kubriko` is sealed with `KubrikoImpl` as its only implementation), and the
`autoInitializingLazy` properties are still initialized after `onInitialize`, which the base class still calls.

## Public API
None (`ViewportManagerImpl` is internal; `onInitialize` is `protected` on the sealed `ViewportManager`, whose
subclasses are not supported for consumers).

## Tests
The existing ones (`ViewportContractTest`, `ViewportManagerInputTest`).

## Verify
`./gradlew :engine:compileKotlinDesktop :engine:desktopTest`

## Manual check
none
