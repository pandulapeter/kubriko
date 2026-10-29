/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import com.pandulapeter.kubrikoCreator.scenes.ProjectClasspath
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameLaunchTest {

    @Test
    fun theClasspathFileSaysHowToLaunchTheGameAfterTheClasspath() {
        val classpath = ProjectClasspath.parse(
            "kubrikoVersion=1.0.0\n/game/classes\n/libs/engine.jar\nlaunch.java=/jdk/bin/java\nlaunch.mainClass=game.MainKt\n" +
                "launch.workingDir=/game\nlaunch.jvmArg=-Da=b=c\nlaunch.jvmArg=-Xmx1g\nlaunch.classpath=/game/classes\nlaunch.classpath=/libs/session.jar\n",
        )
        assertEquals(listOf(File("/game/classes"), File("/libs/engine.jar")), classpath?.files)
        assertEquals(
            GameLaunch(File("/jdk/bin/java"), "game.MainKt", File("/game"), listOf("-Da=b=c", "-Xmx1g"), listOf(File("/game/classes"), File("/libs/session.jar"))),
            classpath?.launch,
        )
    }

    @Test
    fun aClasspathFileWithoutAMainClassOrJavaHasNoLaunch() {
        assertNull(ProjectClasspath.parse("kubrikoVersion=1.0.0\n/game/classes\n")?.launch)
        assertNull(GameLaunch.parse(listOf("launch.java=/java", "launch.workingDir=/game", "launch.classpath=/game")))
    }

    @Test
    fun theCommandStartsTheSessionLauncherInPlayMode() {
        val launch = GameLaunch(File("/jdk/bin/java"), "game.MainKt", File("/game"), listOf("-Xmx1g"), emptyList())
        assertEquals(
            listOf(
                "/jdk/bin/java", "-Xmx1g", "-Dkubriko.creator.port=1234", "-cp", listOf("/a", "/b").joinToString(File.pathSeparator),
                "com.pandulapeter.kubriko.creatorSession.CreatorSessionLauncherKt", "play", "game.MainKt",
            ),
            launch.command(listOf(File("/a"), File("/b")), mapOf("kubriko.creator.port" to "1234")),
        )
    }
}
