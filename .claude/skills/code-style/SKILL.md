---
name: code-style
description: Code style, commenting, and documentation conventions for the Kubriko codebase. MANDATORY — invoke this skill BEFORE writing or editing ANY source in this repo (every Write or Edit to a `.kt`/`.kts` file, new file or change to an existing one), with NO exceptions, even for a "trivial" one-line edit. It governs comment restraint, KDoc on public API, the MPL-2.0 license header on new files, naming, and matching surrounding style — get these right while writing, not after. If you are about to create or modify Kotlin code in Kubriko, you must load this first.
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
- Match the formatting, indentation, and idiom of the surrounding file rather than reformatting to a
  personal preference.

## Always

- Start every new source file with the MPL-2.0 license header (copy from a sibling file).
- Keep the public API stable — optimize under the hood only (see root `CLAUDE.md` → Conventions).
- Honor the performance rules (zero per-frame allocation in hot paths; see root `CLAUDE.md` → Performance).
