# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

import unittest

from check_doc_names import stale_names

PATHS = [
    "data/sync/implementation/src/commonMain/kotlin/com/pandulapeter/kubriko/data/sync/implementation/run/SyncEngine.kt",
    "presentation/src/commonMain/kotlin/com/pandulapeter/kubriko/presentation/ui/screens/songs/SongsScreen.kt",
]
CORPUS = "\n".join(PATHS + [
    "internal class SyncEngine { val MAX_FILE_SIZE = 1; fun rememberElapsed() = Unit }",
    "sealed class SyncState { object Connecting : SyncState() }",
    'val file = "preferences.json"',
])


def stale(text, lines=None):
    return [name for _, name in stale_names(text, CORPUS, PATHS, lines)]


class StaleNamesTest(unittest.TestCase):

    def test_names_the_code_has_are_not_reported(self):
        self.assertEqual([], stale("`SyncEngine` holds `MAX_FILE_SIZE`; `SyncState.Connecting` and `rememberElapsed()`."))

    def test_a_renamed_class_and_a_moved_file_are_reported(self):
        self.assertEqual(["SyncRunEngine", "ui/screens/library"], stale("`SyncRunEngine` in `ui/screens/library`."))

    def test_paths_are_matched_from_wherever_the_note_stands_and_with_what_varies_left_open(self):
        self.assertEqual([], stale("`run/SyncEngine.kt`, `ui/screens/<screen>`, `data/sync/.../SyncEngine.kt`."))

    def test_a_file_the_app_writes_is_known_by_the_code_that_names_it(self):
        self.assertEqual([], stale("`preferences/preferences.json`"))

    def test_prose_commands_and_build_output_are_left_alone(self):
        self.assertEqual([], stale("`true`, `release`, `./gradlew desktopTest`, `{tempo: 120}`, `build/goldens`."))

    def test_only_the_given_lines_are_judged(self):
        self.assertEqual(["MissingThing"], stale("`GoneThing`\n`MissingThing`", lines={2}))


if __name__ == "__main__":
    unittest.main()
