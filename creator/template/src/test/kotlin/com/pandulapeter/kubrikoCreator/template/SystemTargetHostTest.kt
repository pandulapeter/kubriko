/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SystemTargetHostTest {
    private val directory = createTempDirectory().toFile()
    private val project = File(directory, "project").apply { mkdirs() }
    private val home = File(directory, "home")
    private val applications = File(directory, "Applications").apply { mkdirs() }

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun theProjectsOwnSdkComesFirst() {
        val local = sdk("local")
        File(project, "local.properties").writeText("sdk.dir=${local.path.replace("\\", "\\\\").replace(":", "\\:")}\n")
        assertEquals(local, host(environment = mapOf("ANDROID_HOME" to sdk("home").path)).androidSdk(project))
    }

    @Test
    fun theEnvironmentComesBeforeTheDefaultLocation() {
        sdk("home/Library/Android/sdk")
        val fromEnvironment = sdk("environment")
        assertEquals(fromEnvironment, host(environment = mapOf("ANDROID_SDK_ROOT" to fromEnvironment.path)).androidSdk(project))
    }

    @Test
    fun androidStudiosLocationIsFoundOnEachSystem() {
        assertEquals(sdk("home/Library/Android/sdk"), host(osName = "Mac OS X").androidSdk(project))
        assertEquals(sdk("home/Android/Sdk"), host(osName = "Linux").androidSdk(project))
        assertEquals(sdk("appData/Android/Sdk"), host(osName = "Windows 11", environment = mapOf("LOCALAPPDATA" to File(directory, "appData").path)).androidSdk(project))
    }

    @Test
    fun aFolderWithoutPlatformsIsntAnSdk() {
        File(home, "Library/Android/sdk").mkdirs()
        File(project, "local.properties").writeText("sdk.dir=/nowhere\n")
        assertNull(host(osName = "Mac OS X").androidSdk(project))
    }

    @Test
    fun xcodeIsFoundOnlyOnAMac() {
        assertFalse(host(osName = "Mac OS X").canBuildIos)
        File(applications, "Xcode-beta.app").mkdirs()
        assertTrue(host(osName = "Mac OS X").canBuildIos)
        assertFalse(host(osName = "Linux").canBuildIos)
        File(applications, "Xcode-beta.app").deleteRecursively()
        assertTrue(host(osName = "Mac OS X", environment = mapOf("DEVELOPER_DIR" to "/Volumes/Tools/Xcode.app/Contents/Developer")).canBuildIos)
    }

    private fun sdk(path: String) = File(directory, path).apply { File(this, "platforms").mkdirs() }

    private fun host(osName: String = "Linux", environment: Map<String, String> = emptyMap()) = SystemTargetHost(
        environment = environment,
        userHome = home,
        osName = osName,
        applicationsDirectory = applications,
    )
}
