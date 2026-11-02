<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Remove Edit mode and its `tool-scene-editor` dependency from `tool-creator-session`, keeping `SessionMode.EDIT` in the protocol

**Challenged:** amended — the extracted `run` now says it sends the Failure itself (the test reads it from the socket), and takes the log forwarder's scope so the test cancels it instead of leaving a `Logger` collector running in the test JVM.

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** Desktop
**Findings:** INF-15, DOC-17, DOC-20  ·  **Depends on:** none (lands in the same phase as S04; they touch different files, and the Failure-after-Hello order below works with S04's Hello gate)
**Files:** `tools/creator-session/build.gradle.kts`, delete `tools/creator-session/src/desktopMain/kotlin/com/pandulapeter/kubriko/creatorSession/implementation/EditSession.kt` and `tools/creator-session/src/desktopTest/kotlin/com/pandulapeter/kubriko/creatorSession/implementation/EditSessionTest.kt`, `tools/creator-session/src/desktopMain/kotlin/com/pandulapeter/kubriko/creatorSession/CreatorSessionLauncher.kt`, `tools/creator-session/src/desktopMain/kotlin/com/pandulapeter/kubriko/creatorSession/implementation/SessionArguments.kt`, `tools/creator-session/src/commonMain/kotlin/com/pandulapeter/kubriko/creatorSession/protocol/SessionMode.kt` (KDoc only), `tools/creator-session/src/desktopTest/kotlin/com/pandulapeter/kubriko/creatorSession/implementation/SessionArgumentsTest.kt`, new `tools/creator-session/src/desktopTest/kotlin/com/pandulapeter/kubriko/creatorSession/CreatorSessionLauncherTest.kt` (or a case in an existing launcher-level test), `tools/creator-session/README.md`, root `CLAUDE.md` (Tools → `creator-session` line)

## Problem

Creator no longer starts `tool-creator-session` in Edit mode: it edits Scenes in its own window (`creator/CLAUDE.md`, Scene editing: "never a window of its own and not the library's `tools/scene-editor`"), and the only modes it passes are `play` (`workspace/SessionController.kt`: `"kubrikoCreatorMode" to "play"`; the init script: `args("play", run.mainClass.get())`). The published module still ships it:

- `tools/creator-session/build.gradle.kts`:
  ```kotlin
  getByName("desktopMain") {
      dependencies {
          implementation(projects.tools.creatorApi)
          implementation(projects.tools.sceneEditor)
      }
  }
  ```
- `implementation/EditSession.kt` (`SceneEditor.show(...)` with the project's `CreatorEntryPoint`), dispatched from `CreatorSessionLauncher.main`: `is SessionArguments.Edit -> EditSession.run(arguments)`.

So every game played from Creator gets `tool-scene-editor` and what it brings (`tool-debug-menu`, `kotlin-reflect`, `ui-components`, the editor's Compose resources) added to its runtime classpath through the session configuration, for a mode nothing starts. The docs still describe it: root `CLAUDE.md` Tools line ("Edit mode (the Scene editor; Creator itself now edits Scenes in its own window) and Play mode", DOC-17), `tools/creator-session/README.md` ("The process Kubriko Creator starts to run a game or edit its Scenes" and the `edit <scene folder> [scene file]` bullet, DOC-20).

The decision (`decisions.md`: "creator-session Edit mode: remove EditSession + scene-editor dependency in the next library release, keep SessionMode.EDIT in the protocol").

## Fix

This changes a published artifact (`io.github.pandulapeter.kubriko:tool-creator-session`): it ships in the next library release. Its public API doesn't change (`EditSession`, `SessionArguments`, and `SessionFailure` are `internal`; the protocol types stay as they are, `SessionMode.EDIT` included, because the protocol only grows and a newer Creator must still decode an older session's `{"mode":"edit"}` Hello).

1. **build.gradle.kts:** remove `implementation(projects.tools.sceneEditor)`. `projects.tools.creatorApi` stays (`PlayCommandHandler` loads the `CreatorEntryPoint`).
2. **Delete** `EditSession.kt` and `EditSessionTest.kt`.
3. **SessionArguments:** keep parsing `edit` (so the session can say why it won't run), but without the arguments it no longer uses:
   ```kotlin
   /**
    * Edit mode, which this version no longer hosts: Kubriko Creator edits Scenes in its own window.
    */
   data object Edit : SessionArguments {
       override val mode get() = SessionMode.EDIT
   }
   ```
   with `"edit" -> Edit` in `parse`, and the class KDoc becoming "The command line of a session process: `play <main class>` (or `edit`, which is refused)". The unknown-mode message becomes `"Unknown mode: … Expected play."`.
4. **CreatorSessionLauncher.main:** the `Edit` branch fails after the Hello, so an older Creator that still starts Edit mode shows the reason in its console (and a Creator with S04's gate still accepts the connection, since the Hello comes first):
   ```kotlin
   is SessionArguments.Edit -> throw SessionFailure(EDIT_MODE_REMOVED_MESSAGE)
   ```
   with `private const val EDIT_MODE_REMOVED_MESSAGE = "This Kubriko version's session no longer opens the Scene editor; Kubriko Creator edits Scenes in its own window."`. The existing `catch (exception: SessionFailure)` sends `SessionMessage.Failure` and exits with status 1. Update the file's KDoc: "The arguments are `play <main class>`; `edit` is refused with a failure, since Kubriko Creator edits Scenes in its own window." Drop the `EditSession` import.
5. **SessionMode.EDIT KDoc:** "The Scene editor, which sessions no longer host (they answer `edit` with a [SessionMessage.Failure]); kept so that older sessions' messages still decode."
6. **Creator side — keep as is:** `ui/WorkspaceToolbar.kt`'s `SessionMode.EDIT -> Res.string.session_editing` branch, the `session_editing` string (`strings.xml`: "Editing a Scene"), and `ui/SessionStateDescriptionTest.kt`'s EDIT case stay: the `when` must stay exhaustive while the enum value exists, and an older session still sends that Hello. (Alternative, not taken: map EDIT to `session_playing` and delete the string; it saves one string at the cost of a wrong label for an old session.) `creator/src/test/.../session/SessionServerTest.kt` and `tools/creator-session/.../protocol/SessionProtocolTest.kt` keep their `SessionMode.EDIT` Hellos, which test decoding.
7. **Docs (grep the whole repository for `Edit mode`, `edit mode`, `EditSession`, `edit <scene`, `scene folder` and `projects.tools.sceneEditor` after the change; only the lines below should have matched):**
   - Root `CLAUDE.md`, Tools → `creator-session` (DOC-17): replace "at the project's Kubriko version: Edit mode (the Scene editor; Creator itself now edits Scenes in its own window) and Play mode (the game's own `main`), streaming" with "at the project's Kubriko version, to play the game through its own `main`, streaming". The rest of the line stays.
   - `tools/creator-session/README.md` (DOC-20): first sentence "The process Kubriko Creator starts to run a game." Replace the two bullets with one sentence: "`play <main class>` runs the game through its own main function; `edit` (the Scene editor, which Kubriko Creator now has in its own window) is answered with a failure." Change "Either way, the session connects" to "The session connects", and "In play mode it also carries out" to "It also carries out".
   - `creator/CLAUDE.md` already says `kubrikoCreatorSession` runs "in `play` mode" (DOC-1 landed); no change there.
8. **Release notes.** The commit message is what the `release-notes` skill turns into the next release's notes, so make it read as the line under **Creator session tool:** — for example: "Remove the Creator session's Edit mode and its tool-scene-editor dependency, so games played from Kubriko Creator no longer get the Scene editor on their classpath." (one sentence, per the `commit-messages` skill). The bullet the release notes should end up with: "* Edit mode is removed, along with the dependency on `tool-scene-editor`: a session started with `edit` now reports a failure, and games played from Kubriko Creator no longer get the Scene editor, `tool-debug-menu`, or `kotlin-reflect` on their classpath. `SessionMode.EDIT` stays in the protocol."

**Check what Creator itself loses.** Creator depends on `projects.tools.creatorSession` and has no direct `tool-scene-editor` dependency, so the transitive libraries above leave Creator's own runtime classpath too. Creator compiles against `scene-editor-api` through `creator-api`'s `api(projects.tools.sceneEditorApi)` (`Editable`, `EditableMetadata`, `Exposed`), so compilation is unaffected; but compare `./gradlew :creator:dependencies --configuration runtimeClasspath` before and after, and for each library that disappears grep `creator/src/main` for its package. Anything Creator uses at runtime gets an explicit `implementation(...)` in `creator/build.gradle.kts`. A project that depends on one of them itself now loads it from its own classpath in Creator's child loader, which works (the loader is parent-first only for what Creator has).

## Tests

- `SessionArgumentsTest`: replace `editModeTakesASceneFolderAndAnOptionalSceneFile` with `editModeIsStillRecognized` (`SessionArguments.parse(listOf("edit"))` and `parse(listOf("edit", "scenes", "level.json"))` are both `SessionArguments.Edit`); drop the `listOf("edit")` line from `missingOrUnknownArgumentsAreRejected`.
- A launcher test that `edit` sends Hello then Failure: extract the body of `main` between opening the connection and closing it into an `internal fun run(arguments: List<String>, connection: SessionConnection, logScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)): String?`: it sends the Hello, starts `LogForwarder(connection::send, logScope)`, runs the mode, and on a `SessionFailure` sends `SessionMessage.Failure` and prints it to standard error, returning the failure message (null otherwise). `main` keeps only opening the connection, `run`, closing the connection, and `exitProcess(1)` when `run` returned a message. Test `anEditSessionSaysHelloThenFails`: a `ServerSocket(0)` on loopback, `SessionConnection.open(port)`, `run(listOf("edit"), connection, logScope)` with a test-owned `logScope` cancelled in a `finally` (otherwise the forwarder keeps collecting `Logger` for the rest of the test JVM), then read the accepted socket's lines: the first decodes to `Hello(mode = SessionMode.EDIT)`, a later one to `Failure` with the removed-mode message. (Check how `SessionConnectionTest` already opens a real connection and reuse its shape; `LogForwarder` may interleave log lines, so assert on order of the two, not on adjacency.)
- `./gradlew :tools:creator-session:desktopTest` and `:creator:test` pass; the module still builds for every target (`./gradlew :tools:creator-session:build`).

## Manual check

Play the Breakout scratch project from Creator (both the direct launch and, with Hot Reload unavailable, the Gradle session task path if reachable): the game starts, its log reaches the console, and `press_keys`/`capture_game` still work; this needs the library published to Maven Local first (`./gradlew :creator:run` does it).
