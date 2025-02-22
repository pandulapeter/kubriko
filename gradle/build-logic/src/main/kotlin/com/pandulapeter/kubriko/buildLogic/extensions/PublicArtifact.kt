/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.buildLogic.extensions

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Project

internal fun Project.configurePublicArtifact(
    extension: MavenPublishBaseExtension,
    artifactId: String,
) = extension.apply {
    publishToMavenCentral(SonatypeHost.S01)
    signAllPublications()
    coordinates(
        groupId = "io.github.pandulapeter.kubriko",
        artifactId = artifactId,
        version = project.version.toString(),
    )
    pom {
        name.set("Kubriko")
        description.set("Compose Multiplatform game engine for 2D Android, iOS, Desktop, and Web projects.")
        inceptionYear.set("2025")
        url.set("https://github.com/pandulapeter/kubriko")
        licenses {
            license {
                name.set("Mozilla Public License, Version 2.0")
                url.set("https://www.mozilla.org/en-US/MPL/2.0/")
                distribution.set("https://www.mozilla.org/en-US/MPL/2.0/")
            }
        }
        developers {
            developer {
                id.set("pandulapeter")
                name.set("Pandula Péter")
                url.set("https://pandulapeter.github.io/")
                email.set("pandulapeter@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/pandulapeter/kubriko")
            connection.set("scm:git:git://github.com/pandulapeter/kubriko.git")
            developerConnection.set("scm:git:ssh://git@github.com/pandulapeter/kubriko.git")
        }
    }
}