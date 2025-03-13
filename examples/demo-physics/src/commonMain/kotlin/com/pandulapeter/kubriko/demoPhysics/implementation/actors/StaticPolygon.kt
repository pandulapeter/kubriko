/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.actors

//internal class StaticPolygon private constructor(state: State) : RigidBody, Visible, Dynamic, Editable<StaticPolygon> {
//    override val body = state.body
//    override val physicsBody = Body(
//        shape = Polygon(body.vertices.map { Vec2(it.x, it.y) }),
//        x = body.position.x,
//        y = body.position.y,
//    ).apply {
//        density = 0f
//        orientation = body.rotation
//    }
//    // TODO: Hacky workaround for incorrect math
//    override val shouldClip = false
//
//    @set:Exposed(name = "isRotating")
//    var isRotating = state.isRotating
//
//    override fun update(deltaTimeInMilliseconds: Int) {
//        if (isRotating) {
//            body.rotation -= (0.002 * deltaTimeInMilliseconds).toFloat().rad
//            physicsBody.orientation = body.rotation
//        }
//    }
//
//    override fun DrawScope.draw() {
//        val path = Path().apply {
//            moveTo(body.vertices[0].x.raw + body.pivot.x.raw, body.vertices[0].y.raw + body.pivot.y.raw)
//            for (i in 1 until body.vertices.size) {
//                lineTo(body.vertices[i].x.raw + body.pivot.x.raw, body.vertices[i].y.raw + body.pivot.y.raw)
//            }
//            close()
//        }
//        drawPath(
//            path = path,
//            color = Color.DarkGray,
//            style = Fill,
//        )
//        drawPath(
//            path = path,
//            color = Color.Black,
//            style = Stroke(width = 2f),
//        )
//    }
//
//    override fun save() = State(
//        body = body,
//        isRotating = isRotating,
//    )
//
//    @kotlinx.serialization.Serializable
//    data class State(
//        @SerialName("body") val body: SerializablePolygonBody = PolygonBody(),
//        @SerialName("isRotating") val isRotating: Boolean = false,
//    ) : Serializable.State<StaticPolygon> {
//
//        override fun restore() = StaticPolygon(this)
//
//        override fun serialize() = Json.encodeToString(this)
//    }
//}
