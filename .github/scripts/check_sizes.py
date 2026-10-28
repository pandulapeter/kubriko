# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

"""
Keeps a change from pushing a file or a function past the sizes the code-style skill sets.

Main Kotlin sources (tests are left out) should be at most FILE_LINES lines long, and a top-level function — a
Composable above all — at most FUNCTION_LINES. Data tables can be exempted by name in EXEMPT.

Only crossing a limit fails: a Kotlin file or function within it before the change and past it after. What was already
past it may grow or shrink, since splitting it is a refactor of its own that a review sweep plans, so no list of
exceptions is kept and nothing is to be updated when a file moves or shrinks. A file git sees as renamed is judged
against its old path, and a function against its old self wherever in the change it was.

With no arguments it judges the staged change against HEAD (.githooks/pre-commit); `--base <rev>` judges the working
tree against that revision (tests.yml, on a pull request or a push to main). `--hook` is the Claude Code edit hook
(.claude/settings.json): it reads the hook's JSON from stdin and judges the file the edit wrote against HEAD, so that a
file is split while it is being written; an edit that grows what is already past its limit gets a note instead. `--shell-hook` is the Claude Code hook after a shell command, which may have written any
file without the edit hook seeing it: it judges the working tree against HEAD, files git does not track yet included,
and reports failures only, since a note would repeat after every command.
"""

import fnmatch
import json
import os
import re
import subprocess
import sys

FILE_LINES = 500
FUNCTION_LINES = 120

EXEMPT = ()


DECLARATION = re.compile(
    r"^(?:(?:public|internal|private|protected|inline|suspend|operator|infix|override|actual|expect|tailrec|external)\s+)*fun\s"
)
FUNCTION_NAME = re.compile(r"fun\s+(?:<[^>]*>\s*)?([\w.`]+(?:<[^>]*>)?[\w.`]*)\s*\(")
TEST_SOURCES = re.compile(r"/src/\w*[Tt]est/")
CONTINUATION = tuple(".?:+-*/&|=")


def is_main_kotlin(path):
    return path.endswith(".kt") and "/src/" in path and not TEST_SOURCES.search(path) and not any(
        fnmatch.fnmatch(path, pattern) for pattern in EXEMPT
    )


def code_only(lines):
    """The lines with strings, character literals and comments taken out, so that only real brackets are counted."""
    result = []
    in_comment = False
    in_raw_string = False
    for line in lines:
        kept = []
        index = 0
        while index < len(line):
            if in_comment or in_raw_string:
                end = line.find("*/" if in_comment else '"""', index)
                if end < 0:
                    break
                index = end + (2 if in_comment else 3)
                in_comment = in_raw_string = False
            elif line.startswith("//", index):
                break
            elif line.startswith("/*", index):
                in_comment = True
                index += 2
            elif line.startswith('"""', index):
                in_raw_string = True
                index += 3
            elif line[index] in "\"'":
                quote = line[index]
                index += 1
                while index < len(line) and line[index] != quote:
                    index += 2 if line[index] == "\\" else 1
                index += 1
            else:
                kept.append(line[index])
                index += 1
        result.append("".join(kept))
    return result


def top_level_functions(text):
    """(name, first line, length in lines) of every function declared at the top level of a Kotlin file."""
    lines = text.split("\n")
    code = code_only(lines)
    functions = []
    start = 0
    while start < len(code):
        if not DECLARATION.match(code[start]):
            start += 1
            continue
        depth = 0
        has_opened = False
        end = start
        while end < len(code):
            for character in code[end]:
                if character in "([{":
                    depth += 1
                    has_opened = True
                elif character in ")]}":
                    depth -= 1
            if has_opened and depth == 0 and not code[end].rstrip().endswith("="):
                following = next((line for line in code[end + 1:] if line.strip()), "")
                if not (following[:1].isspace() and following.strip().startswith(CONTINUATION)):
                    break
            end += 1
        name = FUNCTION_NAME.search(lines[start])
        functions.append((name.group(1) if name else lines[start].strip(), start + 1, end - start + 1))
        start = end + 1
    return functions


def measure(paths, read):
    """Every size the check limits, as {key: (size, limit, unit)}; a function's key is "<path>::<name>"."""
    sizes = {}
    for path in paths:
        if is_main_kotlin(path):
            text = read(path)
            sizes[path] = (text.count("\n") + (0 if text.endswith("\n") else 1), FILE_LINES, "lines")
            for name, _, length in top_level_functions(text):
                key = f"{path}::{name}"
                if key not in sizes or sizes[key][0] < length:
                    sizes[key] = (length, FUNCTION_LINES, "lines")
    return sizes


def judged(before, after):
    """(failures, notes) of a change, from what measure() gives for the files it touched before and after it."""
    failures = []
    notes = []
    for key, (size, limit, unit) in sorted(after.items()):
        if size <= limit:
            continue
        previous = before.get(key)
        if previous is None and "::" in key:
            # A function moved to another file in the same change is still the function it was.
            name = key.split("::", 1)[1]
            previous = next((value for other, value in before.items() if other.endswith(f"::{name}")), None)
        if previous is not None and previous[0] > limit:
            notes.append(f"{key} is {size} {unit}, past the limit of {limit}; if the change added a responsibility, "
                         "give it a file or function of its own.")
        else:
            failures.append(f"{key} is {size} {unit}, past the limit of {limit}; split it by cohesion (see the "
                            "code-style skill).")
    return failures, notes


def git(*arguments):
    return subprocess.run(["git", *arguments], check=True, capture_output=True).stdout.decode("utf-8", "replace")


def shown(revision, path):
    """The content of a path at a revision ("" for the index), or "" when it did not exist there."""
    result = subprocess.run(["git", "show", f"{revision}:{path}"], capture_output=True)
    return result.stdout.decode("utf-8", "replace") if result.returncode == 0 else ""


def changed(base, staged):
    """{new path: old path or None} of every file added, modified or renamed since a revision."""
    fields = git("diff", "--name-status", "-M", "-z", *(["--cached"] if staged else []), base).split("\0")
    paths = {}
    index = 0
    while index < len(fields) and fields[index]:
        status = fields[index]
        if status[0] in "RC":
            paths[fields[index + 2]] = fields[index + 1] if status[0] == "R" else None
            index += 3
        else:
            if status[0] in "AM":
                paths[fields[index + 1]] = fields[index + 1] if status[0] == "M" else None
            index += 2
    return paths


def read_file(path):
    with open(path, encoding="utf-8", errors="replace") as file:
        return file.read()


def judged_change(paths, base, read_after):
    """judged() for {new path: old path or None}, the old content read at the base revision."""
    before = measure(list(paths), lambda path: shown(base, paths[path]) if paths[path] else "")
    return judged(before, measure(list(paths), read_after))


def checked_edit(hook_input):
    """For the Claude Code hook: (failures, notes) of the one file an edit wrote, against HEAD in its own checkout."""
    tool_input = hook_input.get("tool_input") or {}
    path = tool_input.get("file_path") or ""
    if not path or not os.path.isfile(path):
        return [], []
    top = subprocess.run(
        ["git", "-C", os.path.dirname(path), "rev-parse", "--show-toplevel"], capture_output=True, text=True,
    ).stdout.strip()
    if not top:
        return [], []
    os.chdir(top)
    relative = os.path.relpath(os.path.realpath(path), os.path.realpath(top))
    # A file the edit left as HEAD has it is not in the diff, and one HEAD lacks reads as empty there.
    failures, notes = judged_change({relative: changed("HEAD", False).get(relative, relative)}, "HEAD", read_file)
    # Every edit of a file already past its limit would repeat the note; only one that adds lines earns it.
    if "old_string" in tool_input or "edits" in tool_input:
        edits = tool_input.get("edits") or [tool_input]
        if sum(edit.get("new_string", "").count("\n") - edit.get("old_string", "").count("\n") for edit in edits) <= 0:
            notes = []
    return failures, notes


def checked_tree():
    """For the Claude Code shell hook: the failures of the working tree against HEAD, in the checkout around the current
    directory, untracked files included, as a new one is untracked until it is staged; none outside a checkout."""
    top = subprocess.run(["git", "rev-parse", "--show-toplevel"], capture_output=True, text=True).stdout.strip()
    if not top:
        return []
    os.chdir(top)
    paths = changed("HEAD", False)
    for path in git("ls-files", "--others", "--exclude-standard", "-z").split("\0"):
        if path:
            paths.setdefault(path, None)
    failures, _ = judged_change(paths, "HEAD", read_file)
    return failures


def main(arguments):
    if "--shell-hook" in arguments:
        failures = checked_tree()
        if failures:
            print("\n".join(failures), file=sys.stderr)
            return 2
        return 0
    if "--hook" in arguments:
        failures, notes = checked_edit(json.load(sys.stdin))
        if failures:
            print("\n".join(failures + notes), file=sys.stderr)
            return 2
        if notes:
            print(json.dumps({"hookSpecificOutput": {
                "hookEventName": "PostToolUse", "additionalContext": "\n".join(notes),
            }}))
        return 0
    if "--base" in arguments:
        base = arguments[arguments.index("--base") + 1]
        failures, _ = judged_change(changed(base, False), base, read_file)
    else:
        failures, _ = judged_change(changed("HEAD", True), "HEAD", lambda path: shown("", path))
    for problem in failures:
        print(f"::error::{problem}" if "--base" in arguments else problem, file=sys.stderr)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
