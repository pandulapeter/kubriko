<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Make the size check measure every function, members of classes and objects included, under a name qualified by its class, and treat test fixtures as tests

**Challenged:** amended — said how a class header is told from `::class` and `object :` expressions and that a `fun interface` opens a body of its own (both occur in Creator); the cross-class `judged` test passes already and is a guard, not a test that fails today.

**Kind:** process  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** process request P1 (user, 2026-11-02)  ·  **Depends on:** none
**Files:**
- `.github/scripts/check_sizes.py`
- `.github/scripts/test_check_sizes.py`
- `CLAUDE.md` (the **Checks** paragraph)
- `.claude/skills/code-style/SKILL.md` (the "Files and functions stay small" bullet)
- `.claude/settings.json` (the PreToolUse `additionalContext` text only)
- `.github/workflows/tests.yml` (the comment above "Check the file and function sizes" only)

## Problem

`check_sizes.py` limits only top-level functions. Its declaration pattern is anchored at column 0:

```python
DECLARATION = re.compile(
    r"^(?:(?:public|internal|private|protected|inline|suspend|operator|infix|override|actual|expect|tailrec|external)\s+)*fun\s"
)
```

and `top_level_functions(text)` only starts a function on a line that matches it, so a 300-line `override fun update(…)` inside a
class, an `object`, or a `companion object` is never measured. Kotlin Creator code is mostly members (controllers, stores,
commands are classes and objects), so the 120-line rule the code-style skill states ("top-level functions (Composables above all)
under 120") is not enforced where most code lives.

Two smaller faults of the same pattern:
- `internal fun interface TickClock {` (`engine/.../helpers/TickClock.kt:18`; also `scenes/EntryPointLoader.kt:15`,
  `workspace/WorkspaceOpener.kt:18`, `template/WrapperInstaller.kt:17` in Creator) matches `DECLARATION`, so the whole interface is
  measured as a function whose name is the stripped line.
- `TEST_SOURCES = re.compile(r"/src/\w*[Tt]est/")` doesn't match `src/testFixtures/`, which the Creator module split (plans M03 and
  later) introduces through `java-test-fixtures`; fakes there would be held to the main-source limits.

Measured with a prototype of the fix at the current HEAD, the only members already past 120 lines are
`PlanarCuboidRenderer.update` (188 lines, `examples/demo-isometric-graphics/.../planar/actor/PlanarCuboidRenderer.kt:145`) and
`VolumetricCuboidRenderer.update` (192, `.../volumetric/actor/VolumetricCuboidRenderer.kt:114`), both in the module the Tesselar
migration replaces. Since only crossing a limit fails, they need no baseline: an edit to them gets the "already past" note. The
top-level ones found today stay as they are (`WorkspaceScreen` 271 lines, `BlockTypeEditor` 174, both owned by Creator's UI plans).

## Fix

All in `check_sizes.py`; keep the module docstring's description of what fails and what gets a note, and replace "a top-level
function" with "a function (top-level or a member of a class, object, or interface)".

1. **Declarations at any depth.** Allow leading whitespace and annotations on the same line, add the member-only modifiers, and
   exclude `fun interface`:
   ```python
   DECLARATION = re.compile(
       r"^(\s*)(?:@[\w.]+(?:\([^)]*\))?\s+)*(?:(?:public|internal|private|protected|inline|suspend|operator|infix|override|actual|"
       r"expect|tailrec|external|open|abstract|final)\s+)*fun\s+(?!interface\b)"
   )
   ```
   (`fun interfaceScaleLabel(` in `ui/settings/CreatorSettingsDialog.kt:188` must still be a function: `\b` after `interface`
   keeps it.)
2. **Rename `top_level_functions` to `functions`** and make it return `(qualified name, first line, length)` for every function
   that is not inside another function's body: top-level ones and members, at any nesting of classes. A local function (a `fun`
   inside a function body) is part of the function around it and is not listed on its own; scanning resumes after the outer
   function's end line, as today.
3. **The continuation rule compares indentation.** Today an expression body continues while the next non-blank line starts with
   whitespace and a continuation character. For members, use "indented deeper than the declaration line and starting with a
   character of `CONTINUATION`", which is the same rule for top-level functions (indentation 0).
4. **Qualified names.** Track the enclosing `class`/`object`/`interface` (including `enum class`, `data class`, `sealed interface`,
   `companion object` as `Companion`, `object Name`) on a stack keyed by the brace depth at which each body opened, using the
   `code_only` lines so braces in strings and comments don't count. A class header may span lines (constructor parameters);
   push its name at the first `{` that opens at parenthesis depth 0 after the header, and drop a pending header that ends without a
   body (`data class Point(val x: Int)`). A header is a code line that starts, after indentation, annotations, and any modifiers
   (`private`, `inner`, `value`, `annotation`, `abstract`, `open`, `sealed`, `data`, `enum`, `fun`, …), with `class`, `interface`, or
   `object` followed by whitespace and a name; so `KubrikoApi::class.java` (`context/KubrikoApi.kt:20`) is not one, `fun interface
   Clock {` is (`Clock`), and an anonymous `object : ClientSessionOperations {` (`ai/assistant/AgentConversation.kt:83`) pushes no
   name, its members being qualified by the class around it. A member's name is `Outer.Inner.name`, an extension member keeps its receiver
   (`Outer.String.shortened`). Measurement keys stay `"<path>::<name>"`, so two classes' `update` functions are two keys, and
   `judged`'s "moved to another file" fallback (`other.endswith(f"::{name}")`) now matches only the same class-qualified name.
5. **Test fixtures are tests:** `TEST_SOURCES = re.compile(r"/src/\w*[Tt]est(?:Fixtures)?/")`.
6. Keep everything else (the file limit, `EXEMPT`, the hooks, `--base`) unchanged.

Docs, one phrase each:
- `CLAUDE.md` **Checks**: "top-level functions at most 120" → "functions at most 120 (members of classes and objects too)".
- `.claude/skills/code-style/SKILL.md`: "top-level functions (Composables above all) under 120" → "functions (Composables above
  all, and members of classes and objects) under 120".
- `.claude/settings.json`: the same phrase inside the PreToolUse `additionalContext` string (keep the JSON valid; the string is
  inside a shell `jq -n` call, so edit only the words).
- `.github/workflows/tests.yml`: the comment "no Kotlin file or top-level function past" → "no Kotlin file or function past".

After the change, run the new `functions()` over every tracked main Kotlin file (a throwaway loop over `git ls-files '*.kt'`) and
confirm the only members past 120 are the two `update`s above; if a new one shows up (another lane added it), it is already past
and gets a note, so list it in the commit's report rather than splitting it here.

## Tests

In `test_check_sizes.py` (rename the `TopLevelFunctionsTest` class to `FunctionsTest`, keep its three tests with `functions`):
- `test_a_member_function_is_measured_with_its_class_name`: `class Renderer {\n    override fun update(delta: Int) {\n        a()\n    }\n}\n`
  gives `[("Renderer.update", 2, 3)]`.
- `test_members_of_nested_classes_and_companions_are_qualified`: a class with a `companion object { fun create() = 1 }` and an
  inner `class Part { fun draw() {…} }` gives `Outer.Companion.create` and `Outer.Part.draw`.
- `test_a_class_header_spanning_lines_still_names_its_members`: `class Store(\n    val a: Int,\n) : Base {\n    fun save() {\n    }\n}\n`
  gives `Store.save`; a preceding `data class Point(val x: Int)` without a body doesn't become the enclosing class.
- `test_a_local_function_counts_toward_the_function_around_it`: an outer function of 6 lines with a nested `fun helper() {…}` gives
  only the outer one, length 6.
- `test_a_member_expression_body_runs_to_its_last_continuation_line`: `    fun a() = listOf(1)\n        .sum()\n    fun b() = 2\n`
  inside a class gives `a` length 2 and `b` length 1.
- `test_an_abstract_member_is_one_line`: `interface Loader {\n    fun load(path: String): Int\n}` gives `Loader.load` length 1.
- `test_a_fun_interface_is_not_a_function`: `internal fun interface Clock {\n    fun now(): Long\n}\nfun interfaceScale() = 1\n` gives
  `Clock.now` and `interfaceScale`, nothing named `interface`.
- `SizesTest.test_test_fixtures_are_not_measured`: `"m/src/testFixtures/kotlin/A.kt": "x\n" * 900` gives `{}`.
- `JudgedTest.test_a_long_member_of_another_class_is_not_its_past`: before `{"A.kt::A.update": (200, 120, "lines")}`, after
  `{"B.kt::B.update": (200, 120, "lines")}` fails once. `judged` doesn't change, so this passes against the current script too; it
  guards the qualified fallback against a later bare-name match.
- `test_a_class_literal_or_an_anonymous_object_is_not_a_class`: `object Api {\n    val x by lazy { Api::class.java }\n    val y = object : Runnable {\n        override fun run() {\n        }\n    }\n}\n`
  gives `[("Api.run", 4, 2)]`.
- `SizesTest.test_a_long_member_crossing_the_limit_fails`: a class whose member grows from 120 to 121 lines between `before` and
  `after` file texts gives one failure naming `::Renderer.update`.

Run `python3 -m unittest discover -s .github/scripts -p 'test_*.py'`; every new test except the `judged` guard fails against the
current script (member functions aren't found, `fun interface` is reported, testFixtures are measured).
