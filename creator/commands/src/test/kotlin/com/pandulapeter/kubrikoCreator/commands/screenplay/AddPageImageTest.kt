/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.screenplay

import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.FakeProjectRunner
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.PageImages
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(EditorMutation::class)
class AddPageImageTest {
    private val runner = FakeProjectRunner()

    private fun png(width: Int = 20, height: Int = 10) = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", it) }.toByteArray()

    private fun TestScope.context(spriteFiles: SpriteFiles? = null): CommandContext {
        val root = testDirectory()
        return commandContext(rootDirectory = root, runner = runner, spriteFiles = spriteFiles).apply {
            File(root, "screenplay/levels").mkdirs()
            File(root, "screenplay/levels/cave.md").writeText("# Cave\n\nDark.\n\n## Planned\n\n- Bats\n")
            screenplay.refresh()
        }
    }

    private fun CommandContext.files() = project.rootDirectory.walkTopDown().filter { it.isFile }
        .associate { it.relativeTo(project.rootDirectory).invariantSeparatorsPath to if (it.extension == "md") it.readText() else it.readBytes().size.toString() }

    @Test
    fun `an image file is copied next to the page under the next free name, and undo removes both`() = runTest {
        val context = context()
        File(context.project.rootDirectory, "screenplay/levels/Cave-1.png").writeBytes(png())
        val source = File(testDirectory(), "shot.PNG").apply { writeBytes(png()) }
        val after = context.assertRoundTrip(Invocation(AddPageImage, AddPageImage.Input("levels/cave", "The [dark] cave", file = source.path))) { context.files() }
        assertEquals("# Cave\n\nDark.\n\n![The \\[dark\\] cave](cave-2.png)\n\n## Planned\n\n- Bats\n", after.getValue("screenplay/levels/cave.md"))
        assertEquals(png().size.toString(), after.getValue("screenplay/levels/cave-2.png"))
    }

    @Test
    fun `a large image is scaled down before it is written`() = runTest {
        val context = context()
        val source = File(testDirectory(), "huge.png").apply { writeBytes(png(width = 4000, height = 1000)) }
        AddPageImage.apply(context, AddPageImage.Input("levels/cave", "Huge", file = source.path))
        val written = ImageIO.read(File(context.project.rootDirectory, "screenplay/levels/cave-1.png"))
        assertEquals(PageImages.MAXIMUM_WIDTH to 400, written.width to written.height)
    }

    @Test
    fun `a sprite is shown where it is, without a copy`() = runTest {
        val root = testDirectory()
        val sprite = File(root, "src/commonMain/composeResources/drawable/bat.png").apply { parentFile.mkdirs() }.apply { writeBytes(png()) }
        val context = commandContext(rootDirectory = root, spriteFiles = SpriteFiles(mapOf("drawable/bat.png" to sprite))).apply {
            File(root, "screenplay").mkdirs()
            File(root, "screenplay/README.md").writeText("# Game\n\n## Cast\n\nBats.\n")
            screenplay.refresh()
        }
        val after = context.assertRoundTrip(Invocation(AddPageImage, AddPageImage.Input("README", "A bat", sprite = "drawable/bat.png", section = "cast"))) { context.files() }
        assertEquals("# Game\n\n## Cast\n\nBats.\n\n![A bat](../src/commonMain/composeResources/drawable/bat.png)\n", after.getValue("screenplay/README.md"))
        assertEquals(2, after.size)
    }

    @Test
    fun `a screenshot comes from the running game, and a game that isn't running is said so`() = runTest {
        val context = context()
        runner.step = GameStep.Done(console = emptyList(), png = png())
        AddPageImage.apply(context, AddPageImage.Input("levels/cave", "The cave", screenshot = true))
        assertEquals(png().size.toLong(), File(context.project.rootDirectory, "screenplay/levels/cave-1.png").length())
        runner.step = GameStep.NotPlaying
        assertEquals(Rejection(CommandProblem.GAME_NOT_RUNNING), AddPageImage.apply(context, AddPageImage.Input("levels/cave", "Again", screenshot = true)).failure)
    }

    @Test
    fun `what can't be shown is refused, leaving the page as it was`() = runTest {
        val context = context()
        val before = context.files()
        val notAnImage = File(testDirectory(), "notes.png").apply { writeText("not an image") }
        assertEquals(Rejection(CommandProblem.PAGE_IMAGE_SOURCE), AddPageImage.validate(context, AddPageImage.Input("levels/cave", "x")))
        assertEquals(Rejection(CommandProblem.PAGE_IMAGE_SOURCE), AddPageImage.validate(context, AddPageImage.Input("levels/cave", "x", screenshot = true, file = "a.png")))
        assertEquals(Rejection(LookupProblem.PAGE_MISSING, "forest"), AddPageImage.validate(context, AddPageImage.Input("forest", "x", screenshot = true)))
        assertEquals(Rejection(CommandProblem.PAGE_IMAGE_UNREADABLE, notAnImage.path), AddPageImage.apply(context, AddPageImage.Input("levels/cave", "x", file = notAnImage.path)).failure)
        assertEquals(Rejection(CommandProblem.PAGE_IMAGE_UNREADABLE, "/nowhere/a.png"), AddPageImage.apply(context, AddPageImage.Input("levels/cave", "x", file = "/nowhere/a.png")).failure)
        val source = File(testDirectory(), "a.png").apply { writeBytes(png()) }
        assertEquals(
            Rejection(LookupProblem.PAGE_SECTION_MISSING_LISTED, "levels/cave", "Enemies", "Planned"),
            AddPageImage.apply(context, AddPageImage.Input("levels/cave", "x", file = source.path, section = "Enemies")).failure,
        )
        assertEquals(before, context.files())
        assertNull(AddPageImage.validate(context, AddPageImage.Input("levels/cave", "x", file = source.path)))
    }
}
