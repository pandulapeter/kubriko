---
name: code-style
description: Code style, commenting, and documentation conventions for the Kubriko codebase. MANDATORY — invoke this skill BEFORE writing or editing ANY source in this repo (every Write or Edit to a `.kt`/`.kts` file, new file or change to an existing one), with NO exceptions, even for a "trivial" one-line edit. It governs comment restraint, KDoc on public API, the MPL-2.0 license header on new files, naming, one file per Composable and small cohesive files and packages, and matching surrounding style — get these right while writing, not after. If you are about to create or modify Kotlin code in Kubriko, you must load this first.
---

# Kubriko code style

Conventions for matching the pre-established style of this codebase. Architecture and module
layout live in the root `CLAUDE.md`; this skill is specifically about how the code reads.

## Comments

The guiding principle is restraint. Prefer clear names and small, legible functions over prose
that explains what the code already says.

- **Don't narrate routine code.** A normal `remember`/`LaunchedEffect`/`if` block needs no comment.
  If a name like `isReadyToRender` or `CROSSFADE_SETTLE_DELAY` already conveys intent, leave it at that.
- **A short comment is fine for genuinely non-obvious "why".** One or two lines explaining a decision
  that the code cannot express on its own (a sync subtlety, a platform quirk, an ordering constraint)
  is welcome. Existing good examples: the tick-cadence `renderState` snapshot note and the hidden
  minimap viewport note in `demo-isometric-graphics`.
- **Never explain a bug that is already fixed.** Once a fix lands, the surrounding code is just "how
  it works" — do not leave archaeology like "this used to drop frames because…" or multi-line
  postmortems of the old behavior. The only exception is when the fix is an unintuitive solution that
  a future well-meaning optimization would likely undo; then a brief regression-guard note is justified
  (keep it to the constraint, not the history).
- **No comment is better than a redundant or stale one.** When in doubt, rename instead of comment.

## Documentation (KDoc)

- **Public API is documented 100% with KDoc.** Every public/protected declaration in a published
  module (`engine`, `plugins/*`, `tools/*-api`) gets KDoc — types, functions, properties, defaults,
  and the observable behavior of public `StateFlow`s. This is the surface external consumers depend on.
- **Documenting a declaration always uses KDoc (`/** … */`), never a `//` block — even for internal or
  private declarations.** If a class, function, or property warrants a comment about what it is or why
  it exists, that comment is KDoc attached to the declaration. Reserve plain `//` comments for notes on
  statements *inside* a function body.
- **Implementation details are not the public API.** Internal/private code is documented through naming
  and structure, with sparse comments only where truly needed — not exhaustive KDoc on every member.

## Naming and structure

- Prefer descriptive names that remove the need for a comment (`isReadyToRender`, not `flag`).
- Compile-time feature toggles are `private const val` booleans gated with their own `if (TOGGLE)`
  (e.g. `JOYSTICK_ENABLED`, `SHOW_MINI_MAP`). Keep them as standalone conditions; don't fold them into
  a larger boolean expression (it both obscures the toggle and trips the "expression can be simplified"
  inspection).
- **Always use trailing commas** on the last element of any multi-line comma-separated list — function
  parameters and arguments, constructor parameters, collection literals, `enum` entries, `when` with
  multiple guards, etc. This keeps diffs minimal and reordering clean.
- Match the formatting, indentation, and idiom of the surrounding file rather than reformatting to a
  personal preference.

## Files and packages

- **One file, one thing, named after it.** A top-level class, interface or object gets a file of its own;
  small private helpers used only by it stay with it. Shared non-UI helpers (constants, state classes,
  `Modifier` extensions, `remember…` functions) go into a file named for what they are (`EditorMetrics.kt`, `ActorDefaults.kt`) —
  never `Utils.kt` / `Helpers.kt`.
- **Files stay small.** Aim for under ~500 lines. A file that outgrows that is split by cohesion as part of
  the change that grows it: private state classes become `internal` top-level classes in files of their
  own, a pure algorithm moves next to its users in a file named for it. A single algorithm that splitting
  would only scatter may stay whole.
- **Package by role, not by first user.** A building block a second screen, example or tool needs moves
  to a shared package (`tools/ui-components`, a `components` package) rather than being imported out of
  the file that first used it. Tests sit in the package of the code they test.
- **No god objects.** When a class gains a responsibility unrelated to the ones it has, give that
  responsibility a collaborator of its own instead of another dozen members. Interfaces are as narrow as
  their clients (split one that two clients use halves of); a long-lived class takes its
  `CoroutineScope`, dispatcher and clocks rather than building them, so a test can pass virtual time.
- **A behaviour-preserving split moves code verbatim**: the same declarations, each comment with its
  declaration, `private` widened to `internal` only where another file now needs it (check the package for
  a name clash first), and nothing else changed in the same commit.
- **Published modules keep their binary API.** Moving a public top-level function or property to another
  file changes the JVM facade class it compiles into (`FooKt`), which breaks consumers compiled against
  the old one. In `engine`, `plugins/*` and `tools/*-api`, split such files only by keeping the old facade
  name (`@file:JvmName` with `@file:JvmMultifileClass`) or in a release that is allowed to break binary
  compatibility; classes and objects can move between files freely.

## Composable structure

- **Every top-level UI Composable that is not `private` lives in a file of its own, named exactly after
  it** (`SceneEditorToolbar` in `SceneEditorToolbar.kt`). Its `private` sub-Composables and helpers used only by it stay in
  that file; the moment a second file needs one, it becomes `internal` and moves to a file of its own.
  Value Composables (`rememberX`, `Modifier` extensions) are not UI Composables: group them by cohesion in
  a file named for them.
- **A Composable reads as a short, flat list of named children**, not a deep tree of layout primitives
  interleaved with logic. When a body grows several conceptually distinct visual groups, extract each into
  a Composable named for what it *is* in the UI (`PropertyInspector`, `ActorList`), not where it sits
  (`MiddleRow`). Extract when a group has a clear standalone name, nests more than ~2 layout levels inside
  its parent, keeps being reordered as a unit, or the parent has outgrown a screenful and holds several
  such groups; a function that is already one focused widget needs no extraction — don't split for its
  own sake.
- **Each extracted Composable takes only the state and callbacks it uses**, plus a `Modifier` when its
  parent positions it. Shared components take state and callbacks, never a whole editor controller or view model; only a screen receives that. Keep lambdas stable (`remember` them or pass method references) so
  lists don't recompose needlessly.
- **Extraction must not change the rendered UI.** A `@Composable` function is *layout-transparent*: one
  that emits several siblings with no new `Row`/`Column`/`Box` wrapper drops those children straight into
  the caller's layout, so a parent `Row(Arrangement.spacedBy(…))` still spaces them exactly as before.
  Preserve this — don't add a wrapper a group didn't have, and don't drop one it relied on. Apply
  parent-scope modifiers (`Modifier.weight(…)`, which needs `RowScope`/`ColumnScope`) at the call site,
  passing the result in as the child's `modifier`.
- **Decisions are pure functions, not Composable bodies.** Geometry, filtering, sorting, formatting —
  anything computed from plain data — is an `internal` function next to its Composable, where a unit test
  can reach it. The Composable only remembers its result and draws it.
- **No service-locator lookups inside a Composable** beyond what the root wires up; a component gets what
  it needs as a parameter or a `CompositionLocal` provided once near the root.

## User-facing strings

- **No hardcoded user-facing strings in UI code.** Every string that appears on screen — `Text`
  content, `contentDescription`, hints, titles, labels, window/dialog titles, button captions — lives
  in a `composeResources/values/strings.xml` file and is read through `stringResource(Res.string.…)`.
  Do not inline string literals in Composables.
- Use parameterized resources (`%1$s`, `%1$d`, `\n`) for any string assembled from dynamic parts rather
  than concatenating a literal with a value; pass the value as a `stringResource` format argument.
- Keys are lower-snake-case and grouped by intent (e.g. `action_*` for buttons/icons, `property_*` for
  the property inspector). Reuse an existing key rather than duplicating the same text.
- Where a value reaches a Composable from a non-Composable site (e.g. an enum entry, an AWT factory
  lambda), store the `StringResource` (not the resolved `String`) and resolve it with `stringResource`
  at the Composable call site.
- Exempt: persistence keys, file names, log/serialization identifiers, numeric/coordinate readouts, and proper
  names shown verbatim (product, library and site names, such as the Licenses screen's dependency names) — these are
  not translatable copy and stay as literals.

## Clean up after changes

- **Never leave anything unused behind.** When a change removes the last usage of a declaration,
  delete the declaration too — don't leave it dangling.
- This includes the whole chain: unused imports, private/internal functions, properties, classes,
  string/drawable resources.
- After editing, search the codebase for each symbol and resource you stopped using and confirm it
  has no remaining references before finishing.

## Refactor old code when a change outgrows it

- **Don't be afraid to refactor existing code when your change makes it better.** A new feature
  often leaves a name, signature, or structure that no longer fits — fix it as part of the change
  rather than bolting on and moving on.
- **Rename when scope changes.** If a Composable called `…Toggles` gains a slider, or a `loadFile`
  helper starts saving too, the old name now lies — rename it (and every call site) to match what it
  now does. Keep the rename complete: no straggler references to the old name.
- Stay within the spirit of the change. This is about leaving touched code cleaner, not a license for
  sweeping unrelated rewrites.

## Keep CLAUDE.md files in sync

- **After a significant change, update the relevant `CLAUDE.md`.** Each module has its own `CLAUDE.md`
  (plus the root one); when a change alters something they describe — module architecture, a state
  holder's responsibilities, a UI surface the dependency graph, data types — update the matching section
  in the same change so the docs never drift from the code.
- **Only when it matters.** Routine edits that don't change any documented behavior need no doc update.
  Match the existing prose style and keep it terse; don't add a `CLAUDE.md` section for something that
  wasn't documented before unless it genuinely warrants one.

## Always

- Start every new source file with the MPL-2.0 license header (copy from a sibling file).
- Keep the public API stable — optimize under the hood only (see root `CLAUDE.md` → Conventions).
- Honor the performance rules (zero per-frame allocation in hot paths; see root `CLAUDE.md` → Performance).
- **Never commit automatically.** Leave edits in the working tree as uncommitted changes so they can be
  reviewed first. Only run `git commit` (or `git add` in preparation for one) when explicitly instructed
  to commit — finishing the code is not an implicit request to commit it.
