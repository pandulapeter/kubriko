# Third review sweep — structure, SOLID, readability, testability

Reviewed at `2480325f` on `main` (2026-10-09). Angle: architecture and internal code quality — SOLID, readability,
developer satisfaction, testability, Compose best practices; in the Showcase app every non-private top-level Composable
in a file of its own. Ground rules from the user: no public API change unless absolutely necessary; behaviour changes
only where they fix a bug; **do the simple refactors now, plan the complex ones** (the `codebase-review` skill's
section 5). The user had uncommitted edits to `README.md` and the iOS xcschemes when the sweep started; no plan touches
them.

Process: six read-only area reviewers (engine + build-logic, plugins, tools, app, games, demos/tests) → six lane writers
that re-verified every finding at `2480325f` and wrote the plans → four fresh challengers. **227 plans: 179 Now
(`<lane>01`–`49`), 48 Planned (`<lane>50`+).**

## Headlines

- **Bugs fixed now:** a scene-editor save of the physics demo's `DynamicChain` grows the chain by a link and shifts it on
  every save/undo (D15); the Showcase's loading gate misses the isometric demo's title and a licence name (A12); the
  scene editor's body inspector picks "the first `PointBody`-typed member" by reflection (T18); the test-input keyboard's
  left Shift is narrow (D37) and its gamepad strings aren't preloaded (D39); the isometric joystick ignores the host's
  insets (D46); Annoyed Penguins logs a manager under the wrong tag (G31).
- **Bugs that need a decision:** desktop `PointerInputManager.tryToMoveHoveringPointer` throws in any app that never set
  the engine's public `windowState` — Tesselar included (E51); `RaycastExplosion` never casts a ray, so it does nothing
  (P50); Space Squadron's own death explosion scores a point (G58); Wallbreaker's top-right corner bounce looks wrong
  (in G59).
- **The big structural plans:** split `ActorManagerImpl` (E50), extract the frame throttle into a tested
  `FrameTickScheduler` (E52), inject dispatchers/clock for virtual-time tests (E53), a shared Skia source set (E55),
  split the scene editor's `EditorController` (T53), a Showcase session object instead of globals (A51), a
  scene-editor connection instead of global flags (G55), an info-panel `CompositionLocal` (G60).
- **Now:** 179 local refactors — file-per-declaration splits (facades kept with `@file:JvmName` +
  `@file:JvmMultifileClass` where a published file splits), Composable extractions, dead code, KDoc for undocumented
  public API, stale `CLAUDE.md` statements, tests for pure logic that had none.

## Index

✱ = amended by the challenge.

| Plan | Fix | Kind | Severity | Class |
|---|---|---|---|---|
| [E01](E01-split-tick-source-implementations-into-their-own-files.md) | Split the four TickSource implementations out of TickSource.kt into files of their own | refactor | low | Now |
| [E02](E02-move-triangle-mesh-buffers-into-its-own-file.md) | Move TriangleMeshBuffers out of TriangleMesh.kt into a file of its own | refactor | low | Now |
| [E03](E03-move-engine-tests-into-the-package-of-the-code-they-test.md) | Move the engine tests that sit in the root package into the package of the code they test | test | low | Now |
| [E04](E04-rename-the-artifact-metadata-extension-and-give-it-its-own-file.md) | Rename the build-logic PublishingExtension to ArtifactMetadataExtension and give it its own file | build | low | Now |
| [E05](E05-fail-the-build-when-a-published-module-sets-no-artifact-id.md) | Fail the build when a published module sets no artifactId instead of publishing it under an empty one | build | low | Now |
| [E06](E06-delete-the-unused-actor-manager-field-of-viewport-manager-impl.md) | Delete the never-read actorManager field of ViewportManagerImpl | refactor | low | Now |
| [E07](E07-move-the-viewport-resize-logic-into-viewport-manager-impl.md) | Move the viewport resize and aspect-ratio scale logic out of InternalViewport into ViewportManagerImpl | refactor | low | Now |
| [E08](E08-extract-the-duplicated-tick-gate-of-internal-viewport.md) | Extract the frame loop's duplicated "may it tick now" condition into one private function | refactor | low | Now |
| [E09](E09-correct-the-stale-statements-in-the-engine-and-build-logic-docs.md) | Correct the stale statements in the engine and build-logic documentation | docs | low | Now |
| [E10](E10-turn-actor-manager-impl-declaration-comments-into-kdoc-and-drop-their-history.md) | Turn ActorManagerImpl's declaration comments into KDoc and drop the fixed-bug history | docs | low | Now |
| [E11](E11-turn-the-remaining-engine-declaration-comments-into-kdoc.md) | Turn the remaining declaration-level `//` comments of the engine into KDoc | docs | low | Now |
| [E50](E50-split-actor-manager-impl-into-batch-processor-culler-and-layers.md) | Split ActorManagerImpl into a batch processor, a culler with the draw caches, and the layer Composables ✱ | refactor | high | Planned |
| [E51](E51-stop-try-to-move-hovering-pointer-crashing-when-no-window-state-is-set.md) | Stop PointerInputManager.tryToMoveHoveringPointer from crashing desktop apps that never set windowState ✱ | bug | medium | Planned |
| [E52](E52-extract-the-frame-throttle-into-a-testable-frame-tick-scheduler.md) | Extract the viewport's frame-throttle algorithm into an internal FrameTickScheduler with synthetic-timestamp tests | refactor | high | Planned |
| [E53](E53-inject-the-engine-dispatcher-and-clock-so-tests-can-use-virtual-time.md) | Let the engine's coroutine context and clocks be injected internally, so tests run on virtual time ✱ | test | medium | Planned |
| [E54](E54-decide-the-fate-of-the-accidental-public-api-in-the-implementation-package.md) | Decide what to do with the undocumented public API in the engine's implementation package, then retire the PlatformUtils files | refactor | medium | Planned |
| [E55](E55-give-the-skia-targets-a-shared-skiko-source-set.md) | Give the Skia-backed targets (desktop, iOS, Wasm) a shared skikoMain source set so their identical actuals exist once ✱ | build | medium | Planned |
| [P01](P01-rename-physics-helpers-to-line-intersection.md) | Rename plugin-physics' `implementation/Helpers.kt` to `LineIntersection.kt` | refactor | low | Now |
| [P02](P02-move-the-collision-narrow-phase-into-its-own-file.md) | Move the private narrow phase out of `CollisionMaskExtensions.kt` into `collision/implementation/NarrowPhase.kt` | refactor | medium | Now |
| [P03](P03-move-shader-uniform-provider-into-its-own-file.md) | Move `ShaderUniformProvider` out of `ModifierExtensions.kt` into a file of its own | refactor | low | Now |
| [P04](P04-split-gamepad-focus-navigation-by-declaration-keeping-its-facade.md) | Split `GamepadFocusNavigation.kt` by declaration, keeping its `GamepadFocusNavigationKt` facade ✱ | refactor | low | Now |
| [P05](P05-delete-the-unused-shadow-casting-and-ray-constructors.md) | Delete the unused `ShadowCasting`, `RayAngleInformation`, `Ray` constructors and `RayInformation.index` | refactor | low | Now |
| [P06](P06-drop-the-redundant-checks-and-self-parameter-from-physics-body-ray-intersection.md) | Drop the redundant on-line re-checks, the self-passing parameter and `else if (true)` from `PhysicsBody` | refactor | low | Now |
| [P07](P07-trim-fixed-bug-history-from-the-physics-manager-comments.md) | Trim the fixed-bug history from `PhysicsManagerImpl`'s comments and turn its declaration comments into KDoc | docs | low | Now |
| [P08](P08-document-the-undocumented-public-api-in-plugin-physics.md) | Document the undocumented public API in plugin-physics and remove `ParticleExplosion`'s stale TODO and `@param` | docs | low | Now |
| [P09](P09-document-the-undocumented-public-api-in-plugin-collision.md) | Document the undocumented public API in plugin-collision ✱ | docs | medium | Now |
| [P10](P10-document-modifier-shader.md) | Document `Modifier.shader` and turn the `hasValueEquality` comment into KDoc | docs | medium | Now |
| [P11](P11-correct-the-particle-manager-cache-size-docs-and-the-particle-state-example.md) | Correct the `ParticleManager` `cacheSize` KDoc and the particles CLAUDE.md example, and document `reuseParticleInternal` | docs | medium | Now |
| [P12](P12-turn-particle-manager-declaration-comments-into-kdoc.md) | Turn `ParticleManagerImpl`'s declaration comments into KDoc and drop the history phrase from `ParticleBatch` | docs | low | Now |
| [P13](P13-add-the-license-headers-and-kdoc-to-sprite-resource.md) | Add the MPL-2.0 headers and KDoc to `SpriteResource` and `toSpriteResource` | docs | medium | Now |
| [P14](P14-remove-the-dead-imports-from-the-desktop-image-loader.md) | Remove the four unused imports from the sprites plugin's desktop `ImageLoader` | refactor | low | Now |
| [P15](P15-share-the-audio-loading-progress-flow-through-audio-cache.md) | Move the loading-progress flow duplicated by `MusicManagerImpl` and `SoundManagerImpl` into `AudioCache` | refactor | low | Now |
| [P16](P16-extract-the-pointer-change-handling-into-a-private-function.md) | Extract the per-change `when` of `PointerInputManagerImpl.pointerInputHandling` into a private function | refactor | low | Now |
| [P17](P17-turn-pointer-input-manager-declaration-comments-into-kdoc.md) | Turn `PointerInputManagerImpl`'s declaration comments into KDoc | docs | low | Now |
| [P18](P18-turn-gamepad-input-declaration-comments-into-kdoc.md) | Turn gamepad-input's declaration comments into KDoc | docs | low | Now |
| [P19](P19-turn-keyboard-input-manager-declaration-comments-into-kdoc.md) | Turn `KeyboardInputManagerImpl`'s latch comment into KDoc and drop "as before" | docs | low | Now |
| [P20](P20-fix-the-stale-statements-in-the-keyboard-input-claude-md.md) | Fix the stale file name, call syntax and fix history in the keyboard-input CLAUDE.md ✱ | docs | low | Now |
| [P21](P21-remove-the-nonexistent-unload-gotcha-from-the-persistence-claude-md.md) | Remove the gotcha about `unloadAll()` / `unload()` from the persistence CLAUDE.md | docs | low | Now |
| [P22](P22-document-the-serialization-type-serializers.md) | Document plugin-serialization's type serializers and their typealiases | docs | low | Now |
| [P50](P50-make-ray-scatter-cast-its-rays.md) | Make `RayScatter.castRays` actually cast `noOfRays` evenly rotated rays, so `RaycastExplosion` affects bodies | bug | medium | Planned |
| [P51](P51-extract-the-sweep-and-prune-broad-phase-into-its-own-class.md) | Extract the sweep-and-prune broad phase from `PhysicsManagerImpl` into an internal class with a direct pair-order test | refactor | medium | Planned |
| [P52](P52-pin-the-collision-raycast-results-with-tests.md) | Pin plugin-collision's raycast results with tests | test | medium | Planned |
| [P53](P53-share-one-polygon-entry-edge-scan-between-the-raycast-variants.md) | Share one polygon entry-edge scan between `raycastPolygonEntryDistance` and `raycastPolygonHit` | refactor | low | Planned |
| [P54](P54-make-the-keyboard-input-manager-testable-and-test-its-latch.md) | Make `KeyboardInputManagerImpl` reachable from tests without composition and test its one-tick latch | test | medium | Planned |
| [P55](P55-make-the-gamepad-input-manager-testable-and-test-its-state-derivation.md) | Let tests hand `GamepadInputManagerImpl` a fake event handler and test its dead zone, triggers and button diff | test | medium | Planned |
| [P56](P56-extract-gamepad-focus-navigation-into-its-own-class.md) | Extract the gamepad focus-navigation loop from `GamepadInputManagerImpl` into an internal `GamepadFocusNavigator` | refactor | low | Planned |
| [T01](T01-move-file-operation-error-into-its-own-file.md) | Move FileOperationError out of EditorController.kt into a file of its own | refactor | low | Now |
| [T02](T02-split-keyboard-helpers-into-camera-key-controls-and-navigate-back-action.md) | Split KeyboardHelpers.kt into CameraKeyControls.kt and NavigateBackAction.kt | refactor | low | Now |
| [T03](T03-rename-platform-helpers-to-scene-file-io.md) | Rename PlatformHelpers.kt to SceneFileIo.kt | refactor | low | Now |
| [T04](T04-move-index-of-replaced-unique-into-unique-actor-replacement.md) | Move indexOfReplacedUnique out of UndoRedoHistory.kt into UniqueActorReplacement.kt, with its test | refactor | low | Now |
| [T05](T05-rename-snap-helpers-to-scene-offset-snapping.md) | Rename SnapHelpers.kt to SceneOffsetSnapping.kt | refactor | low | Now |
| [T06](T06-split-editor-text-title-and-label-into-their-own-files.md) | Split EditorTextTitle and EditorTextLabel out of EditorText.kt into files of their own | refactor | low | Now |
| [T07](T07-move-property-editor-kind-into-its-own-file.md) | Move PropertyEditorKind and its KType matcher out of PropertyEditorMapper.kt into PropertyEditorKind.kt | refactor | low | Now |
| [T08](T08-delete-the-unused-editor-category-composable-and-its-resources.md) | Delete the unused EditorCategory Composable, its strings and drawables, and the stale category TODO | refactor | low | Now |
| [T09](T09-delete-the-unused-editor-button-and-editor-checkbox.md) | Delete the unused EditorButton and EditorCheckbox components | refactor | low | Now |
| [T10](T10-drop-the-never-passed-name-and-suffix-from-editor-slider.md) | Drop the never-passed name and suffix parameters, and their label, from EditorSlider | refactor | low | Now |
| [T11](T11-share-one-draw-transform-viewport-extension-in-the-scene-editor.md) | Replace the two private copies of DrawTransform.transformViewport in the scene editor with one internal extension | refactor | low | Now |
| [T12](T12-move-the-hex-color-helpers-into-hex-color-kt-and-test-them.md) | Move the hex color helpers out of ColorPropertyEditor.kt into HexColor.kt and test them ✱ | test | low | Now |
| [T13](T13-fix-the-is-debut-menu-enabled-typo-in-the-settings-panel.md) | Rename isDebutMenuEnabled / onIsDebutMenuEnabledChanged to isDebugMenuEnabled / onIsDebugMenuEnabledChanged | refactor | low | Now |
| [T14](T14-pass-the-loading-flag-into-editor-overlay-instead-of-the-controller.md) | Pass the loading flag into EditorOverlay instead of the whole EditorController | refactor | low | Now |
| [T15](T15-extract-the-scene-viewport-out-of-editor-user-interface.md) | Extract the debug menu, game viewport and editor overlay block of EditorUserInterface into a private SceneViewport | refactor | low | Now |
| [T16](T16-remember-the-registered-type-id-list-in-editor-user-interface.md) | Remember the registered type id list instead of copying it on every recomposition | refactor | low | Now |
| [T17](T17-remember-the-exposed-property-list-per-actor-class.md) | Discover the selected actor's @Exposed properties once per class instead of on every recomposition | refactor | medium | Now |
| [T18](T18-read-the-edited-body-from-positionable-body-instead-of-reflection.md) | Read the body in BodyPropertyEditor from Positionable.body instead of the first PointBody-typed member found by reflection | bug | low | Now |
| [T19](T19-extract-the-log-entry-text-and-hue-into-tested-functions.md) | Extract the log entry text and source hue out of the LogEntry Composable into tested internal functions ✱ | test | low | Now |
| [T20](T20-move-the-debug-menu-metadata-labels-into-a-string-resource.md) | Move the debug menu's inline metadata labels into a parameterized string resource | refactor | low | Now |
| [T21](T21-merge-the-two-debug-menu-overlay-switches-into-one.md) | Merge BodyOverlaySwitch and CollisionMaskOverlaySwitch into one private OverlaySwitch | refactor | low | Now |
| [T22](T22-hoist-the-debug-menu-singleton-reads-out-of-logs-header.md) | Hoist the InternalDebugMenu and Logger reads out of LogsHeader and DebugMenuContents into DebugMenuContainer | refactor | low | Now |
| [T23](T23-move-small-slider-with-title-into-its-own-file-keeping-the-facade.md) | Move SmallSliderWithTitle into SmallSliderWithTitle.kt, keeping the SmallSliderKt JVM facade ✱ | refactor | low | Now |
| [T24](T24-move-blocker-into-its-own-test-fixtures-file.md) | Move Blocker out of CountingActor.kt into Blocker.kt in test-fixtures | refactor | low | Now |
| [T25](T25-drop-the-unused-kotlinx-datetime-dependency-from-tool-logger.md) | Drop the unused kotlinx-datetime dependency from tool-logger | build | low | Now |
| [T26](T26-document-the-scene-editor-object-and-drop-its-todo-kdoc.md) | Document the SceneEditor object and drop the "TODO: Documentation" KDoc on its overrides | docs | low | Now |
| [T27](T27-document-the-scene-editor-contract-invoke-overloads.md) | Document the second and third invoke overloads of SceneEditorContract and fix the first one's KDoc | docs | low | Now |
| [T28](T28-document-the-undocumented-ui-components-public-api.md) | Add KDoc to KubrikoColors, ShareManager and the resource preloaders in ui-components | docs | low | Now |
| [T29](T29-correct-the-debug-menu-api-claude-md.md) | Correct tools/debug-menu-api/CLAUDE.md: remove the stray license header and list the real overloads | docs | low | Now |
| [T30](T30-correct-the-debug-menu-noop-claude-md.md) | Correct which overloads the debug-menu noop inherits in tools/debug-menu-noop/CLAUDE.md | docs | low | Now |
| [T31](T31-correct-the-debug-menu-claude-md.md) | Correct the panel sizes, DebugMenuContainer's role and the log-viewer data flow in tools/debug-menu/CLAUDE.md | docs | low | Now |
| [T32](T32-correct-the-ui-components-usage-section-of-its-claude-md.md) | Correct "Usage in existing tools" in tools/ui-components/CLAUDE.md ✱ | docs | low | Now |
| [T33](T33-correct-the-scene-editor-api-claude-md.md) | Correct the property-type rendering and IS_SCENE_EDITOR_AVAILABLE notes in tools/scene-editor-api/CLAUDE.md | docs | low | Now |
| [T34](T34-correct-the-scene-editor-noop-claude-md.md) | Correct the IS_SCENE_EDITOR_AVAILABLE guidance in tools/scene-editor-noop/CLAUDE.md | docs | low | Now |
| [T35](T35-correct-the-scene-editor-claude-md.md) | Correct the panel roles, snapshot fields and hex-field focus note in tools/scene-editor/CLAUDE.md ✱ | docs | low | Now |
| [T50](T50-take-the-selection-side-effects-out-of-state-flow-update.md) | Run removeSelectedActor's side effects outside StateFlow.update and read the selection from its source | bug | low | Planned |
| [T51](T51-let-the-editor-controller-own-the-placement-preview-actor.md) | Let EditorController derive and own the placement preview actor instead of OverlayManager assigning it | refactor | medium | Planned |
| [T52](T52-scope-the-scene-editor-drag-state-to-the-editor-instance.md) | Replace the file-level drag state globals in ModifierExtensions.kt with a per-editor ActorDragState and pure drag math | refactor | low | Planned |
| [T53](T53-split-editor-controller-into-focused-collaborators.md) | Split EditorController into a scene document, scene file I/O, a camera animator and a selection holder, with injected scope and clocks ✱ | refactor | medium | Planned |
| [T54](T54-decide-the-fate-of-the-public-float-to-difference.md) | Decide what to do with the accidentally public Float.toDifference() in tool-scene-editor | refactor | low | Planned |
| [T55](T55-decide-what-is-scene-editor-available-means.md) | Decide what IS_SCENE_EDITOR_AVAILABLE means, since both the real and the noop module declare it false | docs | low | Planned |
| [T56](T56-zero-pad-the-debug-menu-log-timestamps.md) | Zero-pad the time in debug menu log entries | bug | low | Planned |
| [G01](G01-split-annoyed-penguins-game-state-holder-impl-into-its-own-file.md) | Move `AnnoyedPenguinsGameStateHolderImpl` out of `AnnoyedPenguinsGameStateHolder.kt` into `AnnoyedPenguinsGameStateHolderImpl.kt`. | refactor | low | Now |
| [G02](G02-split-blockys-journey-game-state-holder-impl-into-its-own-file.md) | Move `BlockysJourneyGameStateHolderImpl` out of `BlockysJourneyGameStateHolder.kt` into `BlockysJourneyGameStateHolderImpl.kt`. | refactor | low | Now |
| [G03](G03-split-space-squadron-game-state-holder-impl-into-its-own-file.md) | Move `SpaceSquadronGameStateHolderImpl` out of `SpaceSquadronGameStateHolder.kt` into `SpaceSquadronGameStateHolderImpl.kt`. | refactor | low | Now |
| [G04](G04-split-wallbreaker-game-state-holder-impl-into-its-own-file.md) | Move `WallbreakerGameStateHolderImpl` out of `WallbreakerGameStateHolder.kt` into `WallbreakerGameStateHolderImpl.kt`. | refactor | low | Now |
| [G05](G05-move-create-annoyed-penguins-game-state-holder-into-its-own-file.md) | Move `createAnnoyedPenguinsGameStateHolder` out of `AnnoyedPenguinsGame.kt` into `AnnoyedPenguinsGameStateHolderFactory.kt`. | refactor | low | Now |
| [G06](G06-move-create-blockys-journey-game-state-holder-into-its-own-file.md) | Move `createBlockysJourneyGameStateHolder` out of `BlockysJourneyGame.kt` into `BlockysJourneyGameStateHolderFactory.kt`. | refactor | low | Now |
| [G07](G07-move-create-space-squadron-game-state-holder-into-its-own-file.md) | Move `createSpaceSquadronGameStateHolder` out of `SpaceSquadronGame.kt` into `SpaceSquadronGameStateHolderFactory.kt`. | refactor | low | Now |
| [G08](G08-move-create-wallbreaker-game-state-holder-into-its-own-file.md) | Move `createWallbreakerGameStateHolder` out of `WallbreakerGame.kt` into `WallbreakerGameStateHolderFactory.kt`. | refactor | low | Now |
| [G09](G09-move-blockys-journey-ui-element-shape-into-its-own-file.md) | Move `BlockysJourneyUIElementShape` out of `BlockysJourneyTheme.kt` into its own file. | refactor | low | Now |
| [G10](G10-move-the-space-squadron-ui-element-shape-and-border-into-one-style-file.md) | Move `SpaceSquadronUIElementShape` and `Modifier.spaceSquadronUIElementBorder()` into `SpaceSquadronUIElementStyle.kt`. | refactor | low | Now |
| [G11](G11-move-the-wallbreaker-button-color-helper-into-its-own-file.md) | Move `createButtonColor` out of `WallbreakerTheme.kt` into `WallbreakerButtonColor.kt`. | refactor | low | Now |
| [G12](G12-make-the-annoyed-penguins-ui-element-shape-private.md) | Make `AnnoyedPenguinsUIElementShape` private to the theme file. | refactor | low | Now |
| [G13](G13-rename-space-squadron-menu-overlay-to-match-its-file.md) | Rename `SpaceSquadronMenuOverlay` to `MenuOverlay` so it matches its file. | refactor | low | Now |
| [G14](G14-extract-the-space-squadron-hud-composables-out-of-ui-manager.md) | Extract the Space Squadron HUD (`ProgressBar`, `ShipStatusBars`, `ScoreIndicator`) out of `UIManager` into the `ui` package. | refactor | low | Now |
| [G15](G15-extract-the-annoyed-penguins-gameplay-hud-into-its-own-composable.md) | Extract the Annoyed Penguins in-game top bar into `GameplayHud`. | refactor | low | Now |
| [G16](G16-compute-the-annoyed-penguins-level-name-list-once.md) | Compute the Annoyed Penguins level-name list once instead of on every recomposition. | refactor | low | Now |
| [G17](G17-extract-the-annoyed-penguins-menu-top-bar-and-level-selector.md) | Extract `MenuTopBar` and `LevelSelector` out of the Annoyed Penguins `MenuOverlay`. | refactor | low | Now |
| [G18](G18-extract-the-wallbreaker-menu-button-rows.md) | Extract the Wallbreaker menu's two button rows into `PrimaryButtons` and `PreferenceButtons`. | refactor | low | Now |
| [G19](G19-extract-the-wallbreaker-score-card.md) | Extract the duplicated Wallbreaker score card into a private `ScoreCard`. | refactor | low | Now |
| [G20](G20-use-the-shared-loading-indicator-in-annoyed-penguins.md) | Use `LoadingIndicator` from `tools/ui-components` for the AnnoyedPenguinsGame loading spinner. | refactor | low | Now |
| [G21](G21-use-the-shared-loading-indicator-in-blockys-journey.md) | Use `LoadingIndicator` from `tools/ui-components` for the BlockysJourneyGame loading spinner. | refactor | low | Now |
| [G22](G22-use-the-shared-loading-indicator-in-space-squadron.md) | Use `LoadingIndicator` from `tools/ui-components` for the SpaceSquadronGame loading spinner. | refactor | low | Now |
| [G23](G23-use-the-shared-loading-indicator-in-wallbreaker.md) | Use `LoadingIndicator` from `tools/ui-components` for the WallbreakerGame loading spinner. | refactor | low | Now |
| [G24](G24-rename-the-space-squadron-menu-dialog-parameters-after-what-they-mean.md) | Rename the Space Squadron menu's dialog flags and callbacks after what they mean. ✱ | refactor | low | Now |
| [G25](G25-collect-space-squadron-focus-state-once.md) | Collect Space Squadron's `isFocused` once instead of twice. | refactor | low | Now |
| [G26](G26-stop-reusing-the-caller-modifier-inside-annoyed-penguins-zoom-slider-and-info-dialog.md) | Stop applying the caller's `modifier` to inner children of the Annoyed Penguins `ZoomSlider` and `InfoDialog`. | refactor | low | Now |
| [G27](G27-stop-reusing-the-caller-modifier-inside-blockys-journey-info-dialog.md) | Stop applying the caller's `modifier` to the inner text of the Blocky's Journey `InfoDialog`. | refactor | low | Now |
| [G28](G28-move-the-annoyed-penguins-scene-editor-title-into-strings.md) | Move the AnnoyedPenguins scene editor window title into `strings.xml`. ✱ | refactor | low | Now |
| [G29](G29-move-the-blockys-journey-scene-editor-title-into-strings.md) | Move the BlockysJourney scene editor window title into `strings.xml`. | refactor | low | Now |
| [G30](G30-delete-dead-code-in-blockys-journey.md) | Delete the commented-out back-navigation branch and the unused counter-clockwise direction in Blocky's Journey. | refactor | low | Now |
| [G31](G31-log-the-annoyed-penguins-foreground-shader-manager-under-the-foreground-tag.md) | Log the Annoyed Penguins foreground `ShaderManager` under the foreground tag. | bug | low | Now |
| [G32](G32-make-the-annoyed-penguins-game-end-timer-a-val.md) | Make the Annoyed Penguins `gameEndTimer` a `val`. | refactor | low | Now |
| [G33](G33-tidy-the-annoyed-penguins-slingshot.md) | Drop the redundant canvas wrapper and pointer check in the Annoyed Penguins `Slingshot`, and move its last property up. | refactor | low | Now |
| [G34](G34-check-the-editor-case-explicitly-in-annoyed-penguins-star-on-removed.md) | Replace the catch-all in Annoyed Penguins `Star.onRemoved` with an explicit editor check. | refactor | low | Now |
| [G35](G35-turn-the-wallbreaker-declaration-comments-into-kdoc.md) | Turn the Wallbreaker `Ball`/`Paddle` declaration comments into KDoc and drop the fixed-bug history. | docs | low | Now |
| [G36](G36-turn-the-space-squadron-declaration-comments-into-kdoc.md) | Turn the Space Squadron `ShipDestination`/`Bullet` declaration comments into KDoc. | docs | low | Now |
| [G37](G37-trim-the-penguin-launch-comment-to-its-constraint.md) | Trim the Annoyed Penguins launch-impulse comment to its constraint. | docs | low | Now |
| [G38](G38-correct-the-annoyed-penguins-claude-md.md) | Correct the Annoyed Penguins `CLAUDE.md` description of `Star` and of the keyboard plugin. | docs | low | Now |
| [G39](G39-correct-the-blockys-journey-claude-md.md) | Correct the Blocky's Journey `CLAUDE.md` description of which managers each instance holds. | docs | low | Now |
| [G40](G40-correct-the-space-squadron-claude-md.md) | Correct the Space Squadron `CLAUDE.md` description of `ShipAnimationWrapper`. | docs | low | Now |
| [G41](G41-correct-the-wallbreaker-claude-md.md) | Correct the Wallbreaker `CLAUDE.md` description of the background instance. | docs | low | Now |
| [G42](G42-drop-the-unused-debug-menu-dependency-from-annoyed-penguins.md) | Drop the unused debug-menu dependency from `game-annoyed-penguins`. | build | low | Now |
| [G43](G43-drop-the-unused-debug-menu-dependency-from-blockys-journey.md) | Drop the unused debug-menu dependency from `game-blockys-journey`. | build | low | Now |
| [G44](G44-drop-the-unused-debug-menu-dependency-from-space-squadron.md) | Drop the unused debug-menu dependency from `game-space-squadron`. | build | low | Now |
| [G45](G45-drop-the-unused-debug-menu-dependency-from-wallbreaker.md) | Drop the unused debug-menu dependency from `game-wallbreaker`. | build | low | Now |
| [G50](G50-share-the-duplicated-game-ui-helpers-through-examples-shared.md) | Share the duplicated game UI helpers (hover tracking, font typography) through `examples/shared`. | refactor | low | Planned |
| [G51](G51-make-each-games-back-navigation-a-tested-pure-function.md) | Turn each game's `navigateBack` decision tree into a pure, unit-tested function. | test | medium | Planned |
| [G52](G52-give-game-ui-events-intention-named-manager-methods.md) | Move each game's UI-event handling and click sounds out of the entry Composables into intention-named manager methods. | refactor | low | Planned |
| [G53](G53-stop-writing-loading-state-during-composition-in-the-games.md) | Stop writing loading state during composition in the games' `LoadingManager`s. | refactor | medium | Planned |
| [G54](G54-give-each-game-entry-composable-a-single-root-and-drop-the-redundant-space-squadron-animated-visibility.md) | Give each game's entry Composable a single root that takes `modifier`, and drop Space Squadron's redundant nested `AnimatedVisibility`. | refactor | low | Planned |
| [G55](G55-replace-the-global-scene-editor-visibility-flags-with-a-scene-editor-connection.md) | Replace the module-global scene-editor flags of the games and demos with a `SceneEditorConnection` owned by the state holder. ✱ | refactor | medium | Planned |
| [G56](G56-give-annoyed-penguins-levels-a-type-with-localised-names.md) | Give Annoyed Penguins levels a type with localised display names instead of using "Map 1/2/3" map keys as labels. ✱ | refactor | low | Planned |
| [G57](G57-decide-what-to-do-with-the-unused-input-managers-in-annoyed-penguins-and-blockys-journey.md) | Decide what to do with the registered-but-unused input managers in Annoyed Penguins and Blocky's Journey. | refactor | low | Planned |
| [G58](G58-stop-the-space-squadron-ship-explosion-from-scoring-a-point.md) | Stop Space Squadron's own ship explosion from scoring a point, and stop the ship pushing HUD state into `UIManager`. ✱ | bug | low | Planned |
| [G59](G59-make-the-wallbreaker-ball-bounce-a-tested-pure-function.md) | Make the Wallbreaker ball bounce a pure, tested function (and decide the top-right corner case). | test | medium | Planned |
| [G60](G60-provide-the-showcase-info-panel-visibility-through-a-composition-local.md) | Provide the Showcase info-panel visibility through a `CompositionLocal` instead of the global `StateHolder.isInfoPanelVisible`. ✱ | refactor | medium | Planned |
| [D01](D01-move-content-shaders-demo-state-holder-impl-into-its-own-file.md) | Move `ContentShadersDemoStateHolderImpl` out of `ContentShadersDemoStateHolder.kt` into `ContentShadersDemoStateHolderImpl.kt`. | refactor | low | Now |
| [D02](D02-move-the-content-shaders-overlay-into-a-stateless-composable.md) | Move `ContentShadersDemoManager`'s overlay UI into `ui/ContentShadersOverlay.kt`, taking only state and callbacks. | refactor | medium | Now |
| [D03](D03-correct-the-should-draw-border-explanation-in-content-shaders-claude-md.md) | Correct the `shouldDrawBorder` explanation in demo-content-shaders' `CLAUDE.md`. | docs | low | Now |
| [D04](D04-add-the-license-header-to-emitter-properties-panel.md) | Add the missing MPL-2.0 license header to `EmitterPropertiesPanel.kt`. | docs | low | Now |
| [D05](D05-move-particles-demo-state-holder-impl-into-its-own-file.md) | Move `ParticlesDemoStateHolderImpl` out of `ParticlesDemoStateHolder.kt` into `ParticlesDemoStateHolderImpl.kt`. | refactor | low | Now |
| [D06](D06-make-emitter-properties-panel-take-state-and-callbacks.md) | Make `EmitterPropertiesPanel` take state and callbacks instead of the whole `ParticlesDemoManager`. | refactor | medium | Now |
| [D07](D07-move-the-particles-overlay-into-a-stateless-composable.md) | Move `ParticlesDemoManager`'s overlay UI into `ui/ParticlesDemoOverlay.kt`, taking only state and callbacks. | refactor | low | Now |
| [D08](D08-correct-the-particle-fade-out-description-in-claude-md.md) | Correct the description of the particle fade-out tail in demo-particles' `CLAUDE.md`. | docs | low | Now |
| [D09](D09-move-performance-demo-state-holder-impl-into-its-own-file.md) | Move `PerformanceDemoStateHolderImpl` out of `PerformanceDemoStateHolder.kt` into `PerformanceDemoStateHolderImpl.kt`. | refactor | low | Now |
| [D10](D10-delete-dead-code-and-narrow-visibilities-in-demo-performance.md) | Delete demo-performance's dead code and narrow the visibility of its actors and Manager property. | refactor | low | Now |
| [D11](D11-stop-restarting-the-mini-map-game-time-collection-every-frame.md) | Remember the filtered `gameTime` flow so the performance mini map stops restarting its collection every frame, and fix the CLAUDE.md claim. ✱ | refactor | medium | Now |
| [D12](D12-move-the-performance-overlay-into-a-stateless-composable.md) | Move `PerformanceDemoManager`'s overlay UI into `ui/PerformanceDemoOverlay.kt`. ✱ | refactor | low | Now |
| [D13](D13-read-the-performance-scene-editor-title-from-string-resources.md) | Read the Performance demo's Scene Editor window title from string resources. | refactor | low | Now |
| [D14](D14-move-physics-demo-state-holder-impl-into-its-own-file.md) | Move `PhysicsDemoStateHolderImpl` out of `PhysicsDemoStateHolder.kt` into `PhysicsDemoStateHolderImpl.kt`. | refactor | low | Now |
| [D15](D15-make-dynamic-chain-save-round-trip.md) | Make `DynamicChain.save()` the inverse of `restore()`, so each editor save/undo stops adding a link and shifting the chain. | bug | medium | Now |
| [D16](D16-delete-static-circles-unused-viewport-manager-and-hide-physics-demo-manager.md) | Delete `StaticCircle`'s unused `viewportManager` and make the physics state holder's `physicsDemoManager` private. | refactor | low | Now |
| [D17](D17-move-the-physics-overlay-into-a-stateless-composable.md) | Move `PhysicsDemoManager`'s overlay UI into `ui/PhysicsDemoOverlay.kt`. | refactor | low | Now |
| [D18](D18-let-action-type-carry-its-icon-and-description.md) | Let `ActionType` carry its icon and content-description resources instead of two parallel `when` blocks. | refactor | low | Now |
| [D19](D19-share-one-random-polygon-vertex-generator-in-demo-physics.md) | Share one random-polygon vertex generator between the physics state holder and `PhysicsDemoManager`. | refactor | low | Now |
| [D20](D20-share-the-polygon-drawing-of-dynamic-and-static-polygon.md) | Share the polygon path drawing of `DynamicPolygon` and `StaticPolygon`. | refactor | low | Now |
| [D21](D21-read-the-physics-scene-editor-title-from-string-resources.md) | Read the Physics demo's Scene Editor window title from string resources. | refactor | low | Now |
| [D22](D22-trim-the-fixed-bug-history-from-the-dynamic-chain-paragraph.md) | Trim the fixed-bug history from demo-physics `CLAUDE.md`'s `DynamicChain` paragraph. | docs | low | Now |
| [D23](D23-move-shader-animations-demo-state-holder-impl-into-its-own-file.md) | Move `ShaderAnimationsDemoStateHolderImpl` out of `ShaderAnimationsDemoStateHolder.kt` into `ShaderAnimationsDemoStateHolderImpl.kt`. | refactor | low | Now |
| [D24](D24-drop-the-never-registered-shader-manager-from-the-shader-animations-state-holder.md) | Drop the never-registered `ShaderManager` from `ShaderAnimationsDemoStateHolderImpl`. | refactor | low | Now |
| [D25](D25-move-controls-state-into-its-own-file-and-fix-its-claude-md-description.md) | Move `ControlsState` out of `ControlsContainer.kt` into `ui/ControlsState.kt`, and correct its `CLAUDE.md` description. | refactor | low | Now |
| [D26](D26-give-controls-container-explicit-parameters.md) | Give `ControlsContainer` explicit parameters instead of a `Pair` and the whole holder map, and name its callback after what it changes. | refactor | low | Now |
| [D27](D27-split-shader-animations-demo-into-named-private-composables.md) | Split `ShaderAnimationsDemo`'s body into named private Composables for the tab row and the unsupported-platform message. | refactor | low | Now |
| [D28](D28-move-audio-test-state-holder-impl-into-its-own-file.md) | Move `AudioTestStateHolderImpl` out of `AudioTestStateHolder.kt` into `AudioTestStateHolderImpl.kt`. ✱ | refactor | low | Now |
| [D29](D29-move-audio-test-noop-state-holder-impl-into-its-own-file.md) | Move `AudioTestStateHolderImpl` out of `AudioTestStateHolder.kt` into `AudioTestStateHolderImpl.kt`. | refactor | low | Now |
| [D30](D30-move-music-controls-out-of-audio-test-manager.md) | Move `MusicControls` and `ControlButton` out of `AudioTestManager` into `ui/MusicControls.kt`, taking state and callbacks. ✱ | refactor | medium | Now |
| [D31](D31-move-collision-test-state-holder-impl-into-its-own-file.md) | Move `CollisionTestStateHolderImpl` out of `CollisionTestStateHolder.kt` into `CollisionTestStateHolderImpl.kt`. ✱ | refactor | low | Now |
| [D32](D32-move-collision-test-noop-state-holder-impl-into-its-own-file.md) | Move `CollisionTestStateHolderImpl` out of `CollisionTestStateHolder.kt` into `CollisionTestStateHolderImpl.kt`. | refactor | low | Now |
| [D33](D33-delete-the-duplicate-draw-of-draggable-collidable-actor.md) | Delete `DraggableCollidableActor`'s duplicate `draw()` override and make `collisionTestManager` private. | refactor | low | Now |
| [D34](D34-move-input-test-state-holder-impl-into-its-own-file.md) | Move `InputTestStateHolderImpl` out of `InputTestStateHolder.kt` into `InputTestStateHolderImpl.kt`. ✱ | refactor | low | Now |
| [D35](D35-move-input-test-noop-state-holder-impl-into-its-own-file.md) | Move `InputTestStateHolderImpl` out of `InputTestStateHolder.kt` into `InputTestStateHolderImpl.kt`. | refactor | low | Now |
| [D36](D36-turn-the-on-screen-keyboard-layout-into-a-constant-and-key-sizing-into-a-function.md) | Turn the on-screen keyboard's layout into a constant and its key sizing into an internal function. | refactor | low | Now |
| [D37](D37-make-the-left-shift-key-wide-on-the-on-screen-keyboard.md) | Make the left Shift key wide on the input test's on-screen keyboard. | bug | low | Now |
| [D38](D38-extract-a-crosshair-drawing-helper-in-input-test-manager.md) | Extract one allocation-free crosshair-drawing helper in `InputTestManager.drawToViewport`. | refactor | low | Now |
| [D39](D39-preload-the-input-tests-gamepad-strings.md) | Preload test-input's gamepad strings in its resource gate like every other on-screen string. | bug | low | Now |
| [D40](D40-move-isometric-graphics-demo-state-holder-impl-into-its-own-file.md) | Move `IsometricGraphicsDemoStateHolderImpl` out of `IsometricGraphicsDemoStateHolder.kt` into `IsometricGraphicsDemoStateHolderImpl.kt`. | refactor | low | Now |
| [D41](D41-rename-texture-manager-and-make-the-joystick-backing-flows-val.md) | Rename the isometric state holder's `textureManager` to `textureResolver` and make `ControlOverlayManager`'s joystick backing flows `val`. | refactor | low | Now |
| [D42](D42-move-the-mini-map-sampler-and-buffer-into-their-own-files.md) | Move the isometric mini map's `MiniMapSampler` and `MiniMapBuffer` out of `MiniMap.kt` into files of their own. | refactor | low | Now |
| [D43](D43-pass-the-isometric-mini-map-only-what-it-uses.md) | Pass the isometric `MiniMap` only what it reads instead of the whole state holder. ✱ | refactor | low | Now |
| [D44](D44-document-the-mini-map-declarations-with-kdoc.md) | Turn the isometric mini map's declaration comments into KDoc. | docs | low | Now |
| [D45](D45-split-isometric-graphics-content-into-named-private-parts.md) | Split `IsometricGraphicsContent` into named private parts and correct its joystick touch-target comment. ✱ | refactor | medium | Now |
| [D46](D46-position-the-joystick-from-the-passed-window-insets.md) | Position the isometric joystick from the `windowInsets` the host passes instead of `WindowInsets.safeDrawing`. | bug | low | Now |
| [D47](D47-bring-demo-isometric-graphics-claude-md-in-line-with-the-code.md) | Bring demo-isometric-graphics' `CLAUDE.md` in line with the code. | docs | low | Now |
| [D50](D50-make-dynamic-chains-link-count-edit-rebuild-the-chain.md) | Make editing `DynamicChain`'s `linkCount` in the scene editor rebuild the whole chain, and settle what `linkCount` counts. | bug | low | Planned |
| [D51](D51-give-the-time-driven-shaders-one-base-and-drop-the-controls-casts.md) | Give the five time-driven animation shaders one shared base and map each demo type to its code and controls without unchecked casts. ✱ | refactor | low | Planned |
| [D52](D52-implement-or-remove-test-audios-loop-button.md) | Decide whether test-audio's permanently disabled loop button gets implemented or removed with its dead resources. ✱ | bug | low | Planned |
| [D53](D53-check-and-drop-test-audios-extra-viewport-insets-padding.md) | Check whether test-audio's extra `windowInsetsPadding` on the `KubrikoViewport` is needed, and drop it if not. | refactor | low | Planned |
| [D54](D54-re-enable-or-delete-the-collision-tests-ray-emitter.md) | Decide whether test-collision's unused `RayEmitter` is re-enabled (with real ray casts) or deleted, and remove the commented-out code either way. | refactor | low | Planned |
| [D55](D55-publish-the-joystick-layout-as-one-immutable-value.md) | Replace the seven mutable layout `var`s the UI pushes into the isometric `ControlOverlayManager` with one immutable joystick layout and a single geometry function. | refactor | low | Planned |
| [D56](D56-split-the-isometric-renderer-files-holding-several-types.md) | Split the isometric renderer files that hold several top-level types, after doing it in Tesselar first. | refactor | low | Planned |
| [D57](D57-share-one-scatter-loop-in-the-isometric-logic-manager.md) | Share one rejection-sampling scatter loop for the isometric demo's NPCs, trees and bushes, after doing it in Tesselar first. | refactor | low | Planned |
| [D58](D58-share-the-expandable-controls-overlay-in-examples-shared.md) | Share the "info panel + expandable bottom-end controls panel + brush button" overlay through `examples/shared`. | refactor | medium | Planned |
| [D59](D59-share-the-shaders-not-supported-fallback.md) | Share the "shaders not supported" fallback and its string between the two shader demos. | refactor | low | Planned |
| [D60](D60-share-the-delayed-loading-overlay-dismissal.md) | Share the "hide the loading overlay 300 ms after the actors arrive" logic between the demos that repeat it. | refactor | low | Planned |
| [A01](A01-move-menu-item-into-its-own-file.md) | Move the internal `MenuItem` Composable out of `Menu.kt` into `MenuItem.kt` | refactor | low | Now |
| [A02](A02-split-the-desktop-title-bar-file-by-declaration.md) | Split the desktop `TitleBar.kt` into one file per declaration | refactor | low | Now |
| [A03](A03-move-showcase-entry-type-into-its-own-file.md) | Move `ShowcaseEntryType` out of `ShowcaseEntry.kt` into `ShowcaseEntryType.kt` | refactor | low | Now |
| [A04](A04-move-the-about-screen-state-holder-into-its-own-file.md) | Move the About screen's state holder out of `AboutScreen.kt` into `AboutScreenStateHolder.kt` | refactor | low | Now |
| [A05](A05-split-the-licenses-screen-state-holder-and-data-into-their-own-files.md) | Split the Licenses screen's state holder, `LicenseType` and `Dependency` out of `LicensesScreen.kt` | refactor | low | Now |
| [A06](A06-move-the-welcome-screen-state-holder-into-its-own-file.md) | Move `WelcomeScreenStateHolder` out of `WelcomeScreen.kt` into `WelcomeScreenStateHolder.kt` | refactor | low | Now |
| [A07](A07-narrow-the-about-and-licenses-state-holders-to-internal.md) | Narrow the About and Licenses state holder factories and interfaces from public to internal | refactor | low | Now |
| [A08](A08-move-the-state-holder-pool-into-its-own-file-and-list-each-factory-once.md) | Move the state holder pool out of `ExampleScreen.kt` into `ShowcaseStateHolders.kt`, listing each factory call once | refactor | medium | Now |
| [A09](A09-move-the-deeplink-mapping-into-its-own-file-and-test-it.md) | Move the deeplink mapping out of `KubrikoShowcase.kt` into `ShowcaseDeeplink.kt` and pin it with a test | test | low | Now |
| [A10](A10-name-and-extract-the-parts-of-showcase-content.md) | Give `ShowcaseContent.kt`'s private Composables names that say what they are, and extract the side menu, the entry content and the menu scroll sync | refactor | low | Now |
| [A11](A11-define-the-per-entry-top-bar-features-once-and-test-them.md) | Define the per-entry feature predicates (debug menu, info button, logo) once, next to `ShowcaseEntry`, and test them | refactor | low | Now |
| [A12](A12-preload-every-entry-title-and-license-name-before-the-showcase-shows.md) | Preload every entry's title and subtitle and every licence name before the Showcase shows, deriving the lists from the enums | bug | low | Now |
| [A13](A13-read-the-desktop-window-title-from-string-resources.md) | Read the desktop window title from a string resource instead of a literal | refactor | low | Now |
| [A14](A14-use-is-windows-for-the-desktop-fullscreen-branch.md) | Use the desktop module's `isWindows` instead of creating a `MetadataManager` to detect Windows | refactor | low | Now |
| [A15](A15-drop-the-no-op-root-path-strip-from-the-web-initial-path.md) | Drop the `removePrefix` that never matches from the web shell's `initialPath` | refactor | low | Now |
| [A16](A16-remove-the-unused-isometric-demo-dependency-from-the-desktop-app.md) | Remove the unused `demo-isometric-graphics` dependency from the desktop app's build file | build | low | Now |
| [A17](A17-drop-the-removed-isometric-scene-editor-from-the-desktop-docs.md) | Drop the removed isometric demo scene editor from the desktop app's CLAUDE.md | docs | low | Now |
| [A18](A18-correct-the-shared-showcase-claude-md.md) | Correct `app/shared/CLAUDE.md`'s entry-point signature, the `ShowcaseEntry` description and where the breakpoints are computed | docs | low | Now |
| [A19](A19-add-the-missing-trailing-commas-in-app.md) | Add the missing trailing commas in `app/` and drop the redundant enum semicolons | refactor | low | Now |
| [A50](A50-give-the-desktop-window-and-its-fullscreen-toggle-their-own-files.md) | Give the desktop window and its fullscreen toggle their own files instead of a local Composable over six loose states in `main()` | refactor | medium | Planned |
| [A51](A51-replace-the-showcase-globals-with-a-session-passed-from-the-root.md) | Replace the file-level selection and state holder globals with one process-scoped `ShowcaseSession` passed down from the root | refactor | medium | Planned |
| [A52](A52-hoist-the-welcome-screen-more-info-state-to-its-callers.md) | Hoist the Welcome screen's "more info" state to its callers and dissolve the namespace-only `WelcomeScreenStateHolder` | refactor | low | Planned |
| [A53](A53-follow-the-live-theme-for-the-showcase-surface-elevations.md) | Follow the theme `KubrikoTheme` is actually in for the Showcase's surface elevations, and define them once | bug | low | Planned |
| [A54](A54-list-the-missing-runtime-dependencies-on-the-licenses-screen.md) | List the runtime dependencies the Licenses screen misses (Jamepad and its SDL2 natives, `jbr-api`, NavigationEvent) | bug | medium | Planned |
| [A55](A55-decide-whether-dependency-names-need-string-resources.md) | Decide whether proper names (the Licenses screen's dependency names) are exempt from the string resource rule | docs | low | Planned |

## Lanes

| Lane | Area (files owned by its Now plans) | Now | Planned |
|---|---|---|---|
| E | `engine/`, `gradle/build-logic/`, root `CLAUDE.md`, `documentation/` | E01–E11 | E50–E55 |
| P | `plugins/*` | P01–P22 | P50–P56 |
| T | `tools/*` | T01–T35 | T50–T56 |
| G | `examples/shared`, `examples/game-*` | G01–G45 | G50–G60 |
| D | `examples/demo-*`, `examples/test-*` (incl. `-noop`) | D01–D47 | D50–D60 |
| A | `app/*` | A01–A19 | A50–A55 |

Now plans run in numeric order within their lane (each plan states what it depends on). No two lanes' Now plans touch
the same file, so all six lanes are cut from the same commit. **Merge order: E → P → T → G → D → A** — the engine and
plugins first because everything compiles against them, `app/` last because it compiles against every example.

Shared-file rules: a lane edits only its own modules' `CLAUDE.md` files; only lane E edits the root `CLAUDE.md` and
`documentation/`. Planned plans cross lanes in places — E51/E54 touch `plugins/pointer-input` and
`app/desktop/CLAUDE.md` (also edited by A02/A17/A50); E55 moves shaders/sprites actuals (after P03/P10/P14); G55 and
G60 reach into demo-performance/demo-physics, the demo overlays and `app/`; E50 edits `examples/demo-physics/CLAUDE.md`
(locate by text, D22 edits it first). Schedule those after the Now lanes merge.

## Decisions (all on Planned plans — awaiting the user)

No Now plan depends on any of these. Recommended option first.

- **E50** split `ActorManagerImpl`: unify the two viewport-bounds tests as one helper with identical arithmetic, as a
  second commit (vs keep both / reuse `isWithinViewportBounds`, which can flip an edge actor); keep the name `Layers`.
- **E51** `windowState` crash: pointer-input returns `false` when `windowState` is unset or its position unspecified (no
  API change; behaviour goes from throwing to `false`) — vs an additive public `isWindowStateInitialized` — vs computing
  the position from `localToScreen` later.
- **E52** `FrameTickScheduler` is a pure class fed time and gate state (vs one holding the managers).
- **E53** inject an internal dispatcher (wrapped in `SupervisorJob`) and a primitive `TickClock` fun interface (not
  `TimeSource`, which would allocate per tick).
- **E54** accidental public API in `implementation`: `InternalViewport` gets KDoc + `@Deprecated(WARNING)`, internal in
  the next binary-breaking release; `Window.isRunningOn…` documented and moved to `BrowserDetection.kt`; `windowState`
  kept in `WindowState.desktop.kt` with the old facade name.
- **E55** shared source set named `skikoMain`; web keeps its bridge through an expect hook.
- **P50** `RaycastExplosion` starts applying impulses (fix, note it in the release notes) — vs document as
  non-functional — vs deprecate.
- **P53** share the raycast edge scan by returning the winning edge index (one extra edge evaluation per hit mask).
- **P55** gamepad test seam as an internal constructor parameter; **P56** extract `GamepadFocusNavigator` after P55.
- **T51** the placement preview keeps following the mouse after a placement (today's behaviour).
- **T54** public `Float.toDifference()`: KDoc + `@Deprecated(WARNING)` now, internal in the next breaking release.
- **T55** `IS_SCENE_EDITOR_AVAILABLE`: document it as a placeholder (vs make it `true` in the real module, vs deprecate).
- **T56** zero-pad the debug menu's log timestamps (`HH:mm:ss.SSS`).
- **A51** Showcase session: create the selected holder in `select()`, pass the session as parameters.
- **A52** the welcome screen's "more info" state lives in the A51 session.
- **A53** elevations: app-only fix reading darkness from the surface colour, after a manual macOS live-theme check.
- **A54** list Jamepad (+ SDL2), `jbr-api` and NavigationEvent unconditionally, after checking each licence.
- **A55** exempt proper names (dependency names) from the strings rule in `code-style`.
- **G50** share only the hover modifier and typography helpers in `examples/shared`.
- **G51** one pure `navigateBack` function per game; the drift between games becomes separate bug plans later.
- **G52** the UI managers own the click sound; **G53** minimal `SideEffect`/`LaunchedEffect` fix.
- **G54** one root `Box(modifier)` per game entry + a manual comparison of each game.
- **G55** the process-scoped pool (A08 / A51) owns one scene-editor connection per entry — decide with A51.
- **G56** a parameterized `Map %1$d` level name; **G57** drop Annoyed Penguins' unused keyboard manager, keep Blocky's
  Journey's managers and fix its info text; **G58** remove the death-explosion point, expose ship state as flows;
  **G59** extract the bounce first, then fix the top-right corner (`+1, -1`) as its own bug commit.
- **G60** info-panel visibility stays process-scoped (A51 session); only the reads move to a `CompositionLocal`.
- **D50** `DynamicChain.linkCount`: drop `@Exposed` from the setter and keep "linkCount + 1 links".
- **D51** shader controls bound by typed lambdas built in the state holder; **D52** implement the loop button;
  **D53** remove the extra padding only if a device check shows no change; **D54** delete `RayEmitter`; **D55**
  `JoystickLayout` data class; **D56/D57** leave the Tesselar-synced renderer and `LogicManager` until the next sync;
  **D58/D59/D60** give `examples/shared` its own compose resources and a shared helper for performance/physics.

## Challenge

Four fresh challengers (E+P, T+A, G, D) tried to break every fix against `2480325f`: **197 sound, 30 amended, 0
dropped, 0 reclassified.** Notable catches: `kotlin.jvm` isn't default-imported in `commonMain`, so the facade-keeping
splits import `JvmName`/`JvmMultifileClass` (P04, T23); E50's culler would have made the public actor flows throw before
init; E53's `TimeSource` clock would allocate per tick; T12's alpha assertion ignored 8-bit channels; D30 would have
decided play/pause from live state instead of the drawn icon; G55/G60's `remember`-based options would reset on
Android configuration changes and the Windows fullscreen toggle; three Verify task names were mangled (D28/D31/D34).

## Checked and found solid

Engine: Manager lifecycle and delegates, `KubrikoImpl` lookup/init/tick dispatch, `SyncStateFlow`, State/Metadata
managers, body AABBs, `TriangleBatch`, `WasmTriangleBridge`, coordinate conversions, build-logic convention plugins.
Plugins: the `sealed class` + `Impl` + `newInstance` pattern, `AudioCache`, `SpriteManagerImpl`, `SpatialHashGrid`,
`PersistedPropertyWrapper`, `CollisionManagerImpl`/`Arbiter`. Tools: the tested scene-editor helpers,
`LocalTextInputFocusReporter`, `RefCountedRegistry`, Logger, real/noop/api splits. App: strings, platform entry points,
`ReviewButton`/`Disclaimer` expect/actuals. Examples: `GameButton`/`gameRipple`, the `StateHolder` contract, per-frame
code, manager sharing between instances, -noop mirroring.

## Dropped after verification

None. Merged: the engine's and plugins' identical Skia actuals → E55; the games' and demos' scene-editor connection →
G55; the games' and demos' info-panel global → G60. Folded: moving `isSceneEditorVisible` into its own file → G55.
Not raised (out of angle): `ParticleManagerImpl.onUpdate` and Annoyed Penguins' `Slingshot.activePenguin` allocate per
tick (performance).

## Manual checks owed

Each plan's "Manual check" section; the ones that matter most: the scene editor (T08–T18, D15 save/undo of the physics
chain), the debug menu (T19–T22), the Showcase on desktop (A02, A10, A13, A14), web (A15) and Android, every game and demo
screen after its overlay extraction (G14–G27, D02–D47), the isometric demo's joystick in the wide windowed layout (D46).
