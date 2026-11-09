# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

"""
Numbers Kubriko Creator's plan files (creator/plan/NN-name.md) 01, 02, 03, and so on again, after a finished one was deleted or
to put them in a new order, and rewrites every reference to them in the Markdown under creator/ (and the roadmap's list
numbers in creator/plan/README.md), so that renumbering costs one command rather than an agent editing every file.

With no arguments the files keep their order. With arguments (the names without their numbers, such as `blocks git`), the
named files come first, in that order, and the rest follow in theirs. Files are moved with `git mv`, so history follows them.
"""

import os
import re
import subprocess
import sys

PLAN_DIRECTORY = os.path.join("creator", "plan")
PLAN_FILE = re.compile(r"^(\d\d)-([a-z0-9-]+)\.md$")
REFERENCE = re.compile(r"\b(\d\d)-([a-z0-9-]+)\.md\b")
ROADMAP_ITEM = re.compile(r"^(\d+)\. (\[[^\]]*\]\()(\d\d)-([a-z0-9-]+)\.md\)", re.MULTILINE)


def renamed(files, order=()):
    """
    The new name of each plan file in [files] (names such as `03-blocks.md`), by its old name: [order]'s names first, then
    the rest in their current order, numbered from 01.
    """
    current = sorted(name for name in files if PLAN_FILE.match(name))
    by_slug = {PLAN_FILE.match(name).group(2): name for name in current}
    repeated = sorted({slug for slug in order if list(order).count(slug) > 1})
    if repeated:
        raise ValueError("Named more than once: " + ", ".join(repeated))
    unknown = [slug for slug in order if slug not in by_slug]
    if unknown:
        raise ValueError("No plan file is called " + ", ".join(unknown))
    ordered = [by_slug[slug] for slug in order] + [name for name in current if PLAN_FILE.match(name).group(2) not in order]
    return {name: "%02d-%s" % (index + 1, PLAN_FILE.match(name).group(2)) + ".md" for index, name in enumerate(ordered)}


def rewritten(text, renames):
    """
    [text] with each reference to a renamed plan file pointing at its new name, and each roadmap item numbered as its file.
    """
    by_slug = {PLAN_FILE.match(old).group(2): new for old, new in renames.items()}

    def reference(match):
        new = by_slug.get(match.group(2))
        return new if new is not None and match.group(0) in renames else match.group(0)

    def roadmap_item(match):
        new = by_slug.get(match.group(4))
        if new is None:
            return match.group(0)
        return "%d. %s%s)" % (int(new[:2]), match.group(2), new)

    return ROADMAP_ITEM.sub(roadmap_item, REFERENCE.sub(reference, text))


def git(*arguments):
    return subprocess.run(["git", *arguments], check=True, capture_output=True, text=True).stdout


def main(arguments):
    renames = renamed(os.listdir(PLAN_DIRECTORY), arguments)
    moving = {old: new for old, new in renames.items() if old != new}
    for old in moving:
        git("mv", os.path.join(PLAN_DIRECTORY, old), os.path.join(PLAN_DIRECTORY, "renumbering-" + old))
    for old, new in moving.items():
        git("mv", os.path.join(PLAN_DIRECTORY, "renumbering-" + old), os.path.join(PLAN_DIRECTORY, new))
    for path in git("ls-files", "creator").splitlines():
        if not path.endswith(".md") or not os.path.exists(path):
            continue
        with open(path, encoding="utf-8") as file:
            text = file.read()
        new_text = rewritten(text, renames)
        if new_text != text:
            with open(path, "w", encoding="utf-8") as file:
                file.write(new_text)
    for old, new in moving.items():
        print("%s -> %s" % (old, new))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
