# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

"""
Checks that every tracked source file starts with the MPL-2.0 header, for tests.yml.

The header is written in each language's own comment syntax (/* */ in Kotlin, # in Python and YAML, <!-- --> in XML
and Markdown), so what is looked for is its text rather than its exact lines: the two sentences every form carries,
within the first lines of the file. A file a tool writes and rewrites on its own (the run configurations, the Gradle
wrapper) cannot carry one, and is allowed by name.
"""

import fnmatch
import os
import subprocess
import sys

HEADER_LINES = (
    "This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.",
    "https://mozilla.org/MPL/2.0/",
)

# How far into a file the header may start: an XML declaration or a shebang comes before it.
HEADER_SEARCH_LINES = 20

CHECKED_EXTENSIONS = (".kt", ".kts", ".py", ".yml", ".yaml", ".js", ".cjs", ".mjs", ".ts", ".tsx", ".sh", ".swift")

ALLOWED = (
    ".idea/*",
    ".run/*",
    "gradle/wrapper/*",
)


def is_checked(path):
    """Whether a tracked file has to carry the header: source, scripts, workflows, XML resources and every CLAUDE.md."""
    if any(fnmatch.fnmatch(path, pattern) for pattern in ALLOWED):
        return False
    name = os.path.basename(path)
    if name == "CLAUDE.md":
        return True
    if path.endswith(".xml"):
        return path.startswith("src/") or "/src/" in path
    return path.endswith(CHECKED_EXTENSIONS)


def has_header(text):
    head = "\n".join(text.splitlines()[:HEADER_SEARCH_LINES])
    return all(line in head for line in HEADER_LINES)


def missing_headers(paths, read):
    """The checked paths whose text, as read returns it, does not carry the header."""
    return [path for path in paths if is_checked(path) and not has_header(read(path))]


def tracked_files():
    output = subprocess.run(["git", "ls-files", "-z"], check=True, capture_output=True).stdout
    # A file deleted from the working tree and not yet committed is still listed, and has nothing left to check.
    return [path for path in output.decode("utf-8").split("\0") if path and os.path.isfile(path)]


def read_file(path):
    with open(path, encoding="utf-8", errors="replace") as file:
        return file.read()


def main():
    missing = missing_headers(tracked_files(), read_file)
    for path in missing:
        print(f"::error file={path}::{path} does not start with the MPL-2.0 header.", file=sys.stderr)
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
