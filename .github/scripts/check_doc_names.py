# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

"""
Lists the names a CLAUDE.md quotes in backticks that the repository no longer has, so that a stale note is found by a
grep instead of by an agent reading every file.

A quoted name is checked when it looks like code: a path (`ui/screens/songs`, `FileNames.kt`), or an identifier with a
capital letter, an underscore or a dot in it (`SyncEngine`, `MAX_FILE_SIZE`, `SyncState.Connecting`, `rememberElapsed`).
It is known when a tracked path ends in it or when the text of a tracked file other than the notes holds it: a file the
app writes at run time (`preferences.json`) is named in the code that writes it. Commands, directives, plain words and
anything with a space in it are left alone, since no grep can tell them apart from prose. A name only a build plugin or
a platform makes up (a generated Gradle task, a system key) is reported too; such a line is read and left.

With paths it reports every stale name in those files and exits 0: the sweep's list of lines to read. `--staged` judges
the staged change (.githooks/pre-commit) and fails only on a stale name in a line the change adds to a CLAUDE.md, so a
note is never written about something that does not exist, while what went stale earlier is left to the next sweep.
"""

import os
import re
import subprocess
import sys

QUOTED = re.compile(r"`([^`\n]+)`")
IDENTIFIER = re.compile(r"^@?[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*(?:\(\))?$")
PATH = re.compile(r"^[\w.@-]+(?:/[\w.@<>*-]*)+$|^[\w@-]+\.(?:kt|kts|swift|js|cjs|mjs|py|sh|xml|json|yml|yaml|html|toml|properties|plist|md|gradle)$")


MAXIMUM_TEXT_BYTES = 1 << 20


def git(*arguments):
    return subprocess.run(["git", *arguments], check=True, capture_output=True, text=True).stdout


def is_checked(name):
    if PATH.match(name):
        # What a build writes is not tracked, and is named only by the tools that write it.
        return not name.startswith("build/")
    if not IDENTIFIER.match(name):
        return False
    bare = name.lstrip("@").removesuffix("()")
    if bare.startswith("_"):
        # The tail of a name spelled out just before it (`MICROSOFT_STORE_TENANT_ID`, `_CLIENT_ID`).
        return False
    # A plain lowercase word (`true`, `en`, `release`) is prose as often as it is code.
    return any(character.isupper() or character in "_." for character in bare[1:])


def is_known(name, corpus, paths):
    if PATH.match(name):
        pattern = re.escape(name.strip("/")).replace(r"\<", "<").replace(r"\>", ">")
        # A path is quoted from wherever its reader stands, and with `<name>` or `*` for what varies.
        pattern = re.sub(r"<[^>]*>|\\\*", "[^/]*", pattern).replace(r"\.\.\.", ".*")
        matcher = re.compile(r"(^|/)" + pattern + r"(/|$)")
        if any(matcher.search(path) for path in paths):
            return True
        # A file the app writes at run time is named by its last part in the code that writes it.
        return re.sub(r"<[^>]*>.*", "", name.strip("/")).rsplit("/", 1)[-1] in corpus
    return all(part in corpus for part in name.lstrip("@").removesuffix("()").split("."))


def stale_names(text, corpus, paths, lines=None):
    """(line number, name) of each quoted name in a CLAUDE.md the code does not have; only `lines` when given."""
    found = []
    for number, line in enumerate(text.splitlines(), start=1):
        if lines is not None and number not in lines:
            continue
        for name in QUOTED.findall(line):
            name = name.strip()
            if is_checked(name) and not is_known(name, corpus, paths):
                found.append((number, name))
    return found


def tracked():
    """The tracked paths, and the text of every tracked file but the notes, joined with the paths themselves."""
    paths = [path for path in git("ls-files").splitlines() if os.path.isfile(path)]
    texts = list(paths)
    for path in paths:
        if path.endswith(".md") or path.startswith("documentation/"):
            continue
        if os.path.getsize(path) > MAXIMUM_TEXT_BYTES:
            continue
        with open(path, "rb") as file:
            content = file.read()
        # Images, fonts, jars and the like name nothing a note could quote.
        if b"\0" not in content:
            texts.append(content.decode("utf-8", errors="ignore"))
    return paths, "\n".join(texts)


def added_lines(path):
    """The line numbers the staged change adds to a file, from its zero-context diff."""
    numbers = set()
    for hunk in re.finditer(r"^@@ -\S+ \+(\d+)(?:,(\d+))? @@", git("diff", "--cached", "-U0", "--", path), re.M):
        start, count = int(hunk.group(1)), int(hunk.group(2) or 1)
        numbers.update(range(start, start + count))
    return numbers


def main(arguments):
    staged = arguments[:1] == ["--staged"]
    if staged:
        targets = [path for path in git("diff", "--cached", "--name-only", "--diff-filter=AM").splitlines()
                   if os.path.basename(path) == "CLAUDE.md"]
        if not targets:
            return 0
    else:
        targets = arguments or [path for path in git("ls-files").splitlines() if os.path.basename(path) == "CLAUDE.md"]
    paths, corpus = tracked()
    report = []
    for target in targets:
        text = git("show", f":{target}") if staged else open(target, encoding="utf-8").read()
        lines = added_lines(target) if staged else None
        report += [f"{target}:{number}: `{name}` is nowhere in the code" for number, name in
                   stale_names(text, corpus, paths, lines)]
    print("\n".join(report)) if report else None
    if staged and report:
        print("Fix or drop the names above before committing (see the codebase-review skill, section 6).",
              file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
