/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.commands.commandContext
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class PageImageChecksTest {

    @Test
    fun `images no page shows and images a page shows that are gone are named`() {
        val findings = PageImageChecks.of(
            files = mapOf(
                "screenplay/levels/cave.md" to "![Map](cave-1.png) ![Gone](cave-2.png) [plan](../plan.png)\n```\n![code](cave-9.png)\n```",
                "screenplay/README.md" to "![Map again](levels/cave-1.png)",
                "docs/design.md" to "![Kept by a document](../screenplay/levels/kept.png) ![Gone, but not a page's](../screenplay/gone.png)",
            ),
            imageFiles = listOf("screenplay/levels/cave-1.png", "screenplay/plan.png", "screenplay/levels/old.png", "screenplay/levels/cave-9.png", "screenplay/levels/kept.png"),
            exists = { it != "screenplay/levels/cave-2.png" && it != "screenplay/gone.png" },
        )
        assertEquals(
            listOf(
                HealthFinding(HealthFinding.Kind.UNUSED_PAGE_IMAGE, RelationSubject.Image("screenplay/levels/cave-9.png")),
                HealthFinding(HealthFinding.Kind.UNUSED_PAGE_IMAGE, RelationSubject.Image("screenplay/levels/old.png")),
                HealthFinding(HealthFinding.Kind.PAGE_IMAGE_MISSING, RelationSubject.Page("levels/cave"), usage = "screenplay/levels/cave-2.png"),
            ),
            findings,
        )
        assertEquals("Page levels/cave: shows screenplay/levels/cave-2.png, which is gone.", findings.last().toString())
        assertEquals("screenplay/levels/old.png: no page or document shows it.", findings[1].toString())
    }

    @Test
    fun `the open project's findings read its screenplay folder`() = runTest {
        val context = commandContext()
        File(context.project.rootDirectory, "screenplay").mkdirs()
        File(context.project.rootDirectory, "screenplay/README.md").writeText("![x](x.png)")
        File(context.project.rootDirectory, "screenplay/y.PNG").writeBytes(byteArrayOf(1))
        assertEquals(
            listOf(
                HealthFinding(HealthFinding.Kind.UNUSED_PAGE_IMAGE, RelationSubject.Image("screenplay/y.PNG")),
                HealthFinding(HealthFinding.Kind.PAGE_IMAGE_MISSING, RelationSubject.Page("README"), usage = "screenplay/x.png"),
            ),
            context.healthFindings(),
        )
    }
}
