<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Prove with a test that a project's released class loader, and every class it defined, can be garbage collected

**Kind:** tests  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** INF-5  ·  **Depends on:** S03 (makes `ClassLoaderEntryPointLoader` a class; this plan adds a parameter to it)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/scenes/ClassLoaderEntryPointLoader.kt` (a `parent` constructor parameter), new `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/scenes/ClassLoaderReleaseTest.kt`, new `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/scenes/leakProbe/LeakProbeEntryPoint.kt` (probe classes), and, only if the test finds a real leak, the file that holds the reference (most likely in `engine/` or `tools/logger`; see Fix 3)

## Problem

Every rebuild of the project loads its classes into a new `ProjectClassLoader` (`SceneEditingController.markOutdated` → `update`), and the old one is released a few seconds later:

```kotlin
private fun release(released: Deferred<LoadResult>) {
    released.cancel()
    releaseScope.launch {
        ...
        delay(RELEASE_DELAY_IN_MILLISECONDS)
        loaded.loaded.close()
    }
}
```

`close()` closes the `URLClassLoader` and deletes the copied jars, but nothing proves the loader becomes unreachable. Any reference from Creator's side to a project class outliving `kubriko.dispose()` (a thread, a `ThreadLocal`, a coroutine, a Compose snapshot observer, a `Logger` listener, a static cache keyed by `KClass`) keeps the whole loader and every class it defined alive, so a long editing session grows by one project's classes per change. `ClassLoaderEntryPointLoaderTest` only checks that `close()` runs.

(The second half of INF-5, Managers created in `SceneView` before `Kubriko.newInstance` throws not being disposed, was verified not to leak: the built-in Managers acquire nothing before `onInitialize`. It needs no change.)

## Fix

1. **Make the parent injectable.** `ClassLoaderEntryPointLoader(rootDirectory: File, private val parent: ClassLoader = ClassLoaderEntryPointLoader::class.java.classLoader)`; `load` passes `parent` to `classLoaderOf` (whose own `parent` parameter defaults to the field). Internal, no behavior change.

2. **The probe** (test sources, package `com.pandulapeter.kubrikoCreator.scenes.leakProbe`): `LeakProbeEntryPoint : CreatorEntryPoint` whose `createSerializationManager()` returns `EditableMetadata.newSerializationManagerInstance(EditableMetadata(typeId = "Probe", deserializeState = { LeakProbeState(it) }, instantiate = { … }))` and whose `createCustomManagers()` returns `listOf(LeakProbeManager())` (a `Manager` subclass with an `onInitialize` that does nothing more), plus a `LeakProbeActor` (`Editable<LeakProbeActor>`, `Visible`, a `BoxBody`) and its `LeakProbeState`, modelled on `SceneViewTest`'s `Box` / `BoxState`. These classes sit on the test classpath, which is Creator's side, so they would be shared rather than defined by the project's loader; the test hides them.

3. **The test** (`ClassLoaderReleaseTest`, plain JUnit, no Skia: nothing is drawn):
   - Copy the probe's `.class` files into `root/game/build/classes` (read each from the test class loader with `getResourceAsStream`, as `aRebuiltFolderDoesntChangeTheClassesLoadedFromItButItsResourcesStayReadable` does; enumerate them by name: the entry point, Manager, Actor, State, and any `$`-named lambdas/companions; listing the package folder through `LeakProbeEntryPoint::class.java.protectionDomain.codeSource.location` when it is a directory is the robust way), and write `META-INF/services/com.pandulapeter.kubriko.creator.CreatorEntryPoint` naming `LeakProbeEntryPoint`.
   - Parent: a loader over the test class loader that refuses the probe package, so the project loader defines those classes itself:
     ```kotlin
     private class HidingClassLoader(parent: ClassLoader) : ClassLoader(parent) {
         override fun loadClass(name: String, resolve: Boolean): Class<*> =
             if (name.startsWith(PROBE_PACKAGE)) throw ClassNotFoundException(name) else super.loadClass(name, resolve)
     }
     ```
   - In a separate non-inlined function (so no local in the test frame keeps a reference): `ClassLoaderEntryPointLoader(root, HidingClassLoader(javaClass.classLoader)).load(ProjectClasspath("1.0.0", listOf(classes)))`, assert `loaded.entryPoint.javaClass.classLoader is ProjectClassLoader` (proves the probe came from the project), create `SceneView(loaded.entryPoint)`, `runBlocking { view.load(listOf(SceneActorEntry("Probe", "{…}", "a"))) }` so a project Actor is in the Kubriko instance, then `view.dispose()`, `loaded.close()`, and return `WeakReference(loaded.entryPoint.javaClass.classLoader)`.
   - Poll: up to 50 times, `System.gc()`, `Thread.sleep(20)`, stop when `reference.get() == null`; `assertNull(reference.get(), "…")`. Allocate a little garbage between rounds (`ByteArray(1 shl 20)`) to push the collector. A bounded poll is deterministic enough for a weak reference whose referent has no strong path; if it is flaky on CI, raise the bound rather than marking it `@Ignore`.
   - A second test `aLoaderStillInUseIsntCollected` holds the `LoadedEntryPoint` strongly through the same poll and asserts the reference is still set, so the first test can't pass vacuously (for example if `get()` were always null).

4. **If the first test fails**, it found a real leak: find the path to it with a heap dump (`jcmd <pid> GC.heap_dump` while the test polls, then the shortest path to GC roots from the `ProjectClassLoader` in a heap analyzer) and fix the holder, not the test. Likely holders to check first: `Logger`'s listeners or log history holding a project object (`tools/logger`), a Kubriko instance's coroutine scope or `TickSource` not cancelled by `dispose()`, `SpriteLoadingEntryPoint`/`ProjectSpriteLoader` caches, and `kotlinx.serialization`'s serializer cache keyed by `KClass`. A fix in `engine/`, `plugins/`, or `tools/` must keep the public API and its observable behavior (root `CLAUDE.md`, Conventions), and becomes part of the next library release. If the leak is in a library, split it into its own commit before the test's.

No CLAUDE.md or string change.

## Tests

The plan is the test: `ClassLoaderReleaseTest.aReleasedProjectClassLoaderIsCollected` (defined, populated, and released as above; fails if anything outlives `dispose()` + `close()`) and `ClassLoaderReleaseTest.aLoaderStillInUseIsntCollected` (guards the first). Run with `./gradlew :creator:test --tests "*ClassLoaderReleaseTest*"` several times in a row before committing.
