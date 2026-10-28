# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

import unittest

from check_license_headers import missing_headers

KOTLIN_HEADER = """/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko
"""

MARKDOWN_HEADER = """<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# engine
"""


def check(files):
    return missing_headers(list(files), files.__getitem__)


class MissingHeadersTest(unittest.TestCase):

    def test_a_file_with_the_header_passes(self):
        self.assertEqual(check({"engine/src/commonMain/kotlin/Kubriko.kt": KOTLIN_HEADER}), [])

    def test_a_file_without_the_header_fails(self):
        self.assertEqual(
            check({"engine/src/commonMain/kotlin/Kubriko.kt": "package com.pandulapeter.kubriko\n"}),
            ["engine/src/commonMain/kotlin/Kubriko.kt"],
        )

    def test_a_file_in_the_allow_list_passes(self):
        self.assertEqual(check({".run/Desktop.run.xml": "<component/>\n", "gradle/wrapper/gradle-wrapper.properties": ""}), [])

    def test_the_html_comment_form_of_claude_md_passes(self):
        self.assertEqual(check({"engine/CLAUDE.md": MARKDOWN_HEADER}), [])

    def test_a_claude_md_without_the_header_fails(self):
        self.assertEqual(check({"engine/CLAUDE.md": "# engine\n"}), ["engine/CLAUDE.md"])

    def test_xml_is_checked_only_under_src(self):
        self.assertEqual(
            check({"app/desktop/proguard-rules.pro.xml": "<Package/>\n", "app/android/src/main/res/values/strings.xml": "<x/>\n"}),
            ["app/android/src/main/res/values/strings.xml"],
        )

    def test_files_of_other_kinds_are_not_checked(self):
        self.assertEqual(check({"README.md": "# Kubriko\n", "app/shared/src/commonMain/composeResources/files/a.json": "{}"}), [])


if __name__ == "__main__":
    unittest.main()
