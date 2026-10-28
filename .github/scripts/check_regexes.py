# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

"""
Fails on a regular expression in Kotlin whose braces only the JVM accepts. Android compiles `Regex` with ICU, which
refuses a `{` that does not start a `{n}`, `{n,}` or `{n,m}` count and any `}` that does not end one, both outside a
character class; the JVM reads them as literal braces, so the desktop tests pass and the app crashes on Android the
first time the expression is built (a top-level `val`: its whole file fails to initialize). Every Kotlin file is
checked, not only those Android runs, since an escaped brace means the same everywhere and code moves between source
sets.

A pattern is what the string literals inside a `Regex(` or `Pattern.compile(` call spell, joined (`"(a|" + "b)"`), or a
literal `.toRegex(` or `.toPattern(` is called on, after any calls without arguments (`.trimIndent()`). A template or
any other code between them (`${OTHER.pattern}`, a `joinToString`) is read as one plain character: a pattern it names
is a literal checked on its own. A pattern built anywhere else, in a `val` passed to the call, is not seen.

With paths it checks those files, without them every tracked Kotlin file; exits 1 on any finding.
"""

import re
import subprocess
import sys

COUNT = re.compile(r"\{\d+(?:,\d*)?\}")
CALL = re.compile(r"\b(?:Regex|Pattern\.compile)\(")
AFTER = re.compile(r"\s*(?:\.\w+\(\)\s*)*\.(?:toRegex|toPattern)\(")
PLACEHOLDER = "X"
ESCAPES = {"n": "\n", "t": "\t", "r": "\r", "b": "\b", "\\": "\\", '"': '"', "'": "'", "$": "$"}


def skip_template(text, index):
    """The index after the `}` closing the `${` template whose `{` is at `index`, skipping the strings inside it."""
    depth = 0
    while index < len(text):
        character = text[index]
        if character == '"':
            index = read_string(text, index)[1]
            continue
        if character == "{":
            depth += 1
        elif character == "}":
            depth -= 1
            if depth == 0:
                return index + 1
        index += 1
    return index


def read_string(text, start):
    """The value of the string literal starting at `start` (templates as one placeholder) and the index after it."""
    raw = text.startswith('"""', start)
    index = start + (3 if raw else 1)
    value = []
    while index < len(text):
        character = text[index]
        if raw and text.startswith('"""', index):
            # A raw string ends at the last quote of a run of three or more: `""""a""""` holds `"a"`.
            end = index + 3
            while end < len(text) and text[end] == '"':
                end += 1
            value.append('"' * (end - index - 3))
            return "".join(value), end
        if not raw and character == '"':
            return "".join(value), index + 1
        if not raw and character == "\\" and index + 1 < len(text):
            following = text[index + 1]
            if following == "u":
                value.append(chr(int(text[index + 2:index + 6], 16)))
                index += 6
            else:
                value.append(ESCAPES.get(following, following))
                index += 2
            continue
        if character == "$" and index + 1 < len(text):
            if text[index + 1] == "{":
                value.append(PLACEHOLDER)
                index = skip_template(text, index + 1)
                continue
            name = re.match(r"[A-Za-z_][A-Za-z0-9_]*", text[index + 1:])
            if name:
                value.append(PLACEHOLDER)
                index += 1 + name.end()
                continue
        value.append(character)
        index += 1
    return "".join(value), index


def tokens(text):
    """(start, end, value) of every comment (value None) and string literal in Kotlin source, in order."""
    index = 0
    while index < len(text):
        if text.startswith("//", index):
            end = text.find("\n", index)
            end = len(text) if end < 0 else end
            yield index, end, None
            index = end
        elif text.startswith("/*", index):
            end = text.find("*/", index + 2)
            end = len(text) if end < 0 else end + 2
            yield index, end, None
            index = end
        elif text[index] == "'":
            end = text.find("'", index + (3 if text.startswith("\\", index + 1) else 2))
            index = len(text) if end < 0 else end + 1
        elif text[index] == '"':
            value, end = read_string(text, index)
            yield index, end, value
            index = end
        else:
            index += 1


def patterns(text):
    """(offset, pattern) of every regular expression spelled by literals in Kotlin source."""
    spans = list(tokens(text))
    starts = {start: (end, value) for start, end, value in spans}
    found = []
    for start, end, value in spans:
        if value is not None and AFTER.match(text, end):
            found.append((start, value))
    for call in CALL.finditer(text):
        if any(start <= call.start() < end for start, end, _ in spans):
            continue
        parts, depth, index = [], 1, call.end()
        while index < len(text) and depth:
            if index in starts:
                end, value = starts[index]
                if value is not None:
                    parts.append(value)
                index = end
                continue
            if text[index] in "([{":
                depth += 1
            elif text[index] in ")]}":
                depth -= 1
            elif not text[index].isspace() and text[index] != "+" and parts[-1:] != [PLACEHOLDER]:
                parts.append(PLACEHOLDER)
            index += 1
        if any(part != PLACEHOLDER for part in parts):
            found.append((call.start(), "".join(parts)))
    return found


def bare_braces(pattern):
    """The offsets in a regular expression of the braces ICU refuses: outside a class and not a count's."""
    found = []
    depth = 0
    index = 0
    while index < len(pattern):
        character = pattern[index]
        if character == "\\":
            if pattern.startswith("Q", index + 1):
                end = pattern.find("\\E", index + 2)
                index = len(pattern) if end < 0 else end + 2
            elif pattern[index + 1:index + 2] in ("p", "P", "N", "x") and pattern.startswith("{", index + 2):
                # A property, a name or a code point in braces (`\p{L}`, `\N{SPACE}`, `\x{1F600}`) is one escape.
                end = pattern.find("}", index + 2)
                index = len(pattern) if end < 0 else end + 1
            else:
                index += 2
            continue
        if character == "[":
            # A `]` first in a class (`[]a]`, `[^]a]`) is a literal one.
            depth += 1
            index += 1
            if depth == 1:
                if pattern.startswith("^", index):
                    index += 1
                if pattern.startswith("]", index):
                    index += 1
            continue
        if character == "]" and depth:
            depth -= 1
        elif depth == 0 and character == "{":
            count = COUNT.match(pattern, index)
            if count:
                index = count.end()
                continue
            found.append(index)
        elif depth == 0 and character == "}":
            found.append(index)
        index += 1
    return found


def findings(text):
    """(line, pattern) of every regular expression in Kotlin source with a brace ICU refuses."""
    return [(text.count("\n", 0, offset) + 1, pattern) for offset, pattern in patterns(text) if bare_braces(pattern)]


def main(arguments):
    paths = arguments or [path for path in subprocess.run(
        ["git", "ls-files", "*.kt", "*.kts"], check=True, capture_output=True, text=True,
    ).stdout.splitlines()]
    report = []
    for path in paths:
        with open(path, encoding="utf-8") as file:
            report += [f"{path}:{line}: {pattern}" for line, pattern in findings(file.read())]
    if report:
        print("\n".join(report))
        print("Escape the braces above (\\{, \\}) that are not a {n,m} count: Android's ICU refuses them.",
              file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
