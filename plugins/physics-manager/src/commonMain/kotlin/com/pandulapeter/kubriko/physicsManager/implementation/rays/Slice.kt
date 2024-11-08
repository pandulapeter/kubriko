package com.pandulapeter.kubriko.physicsManager.implementation.rays

import com.pandulapeter.kubriko.physicsManager.implementation.collision.bodies.CollisionBodyInterface
import com.pandulapeter.kubriko.physicsManager.implementation.dynamics.Body
import com.pandulapeter.kubriko.physicsManager.implementation.dynamics.World
import com.pandulapeter.kubriko.physicsManager.implementation.dynamics.bodies.PhysicalBodyInterface
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.Circle
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.Polygon
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physicsManager.implementation.math.Vec2
import kotlin.math.sqrt

/**
 * Class to allow two polygons to be sliced.
 *
 * @param startPoint The origin of the rays projection.
 * @param direction  The direction of the ray points in radians.
 * @param distance   The distance the ray is projected
 */
class Slice(val startPoint: Vec2, direction: Vec2, distance: Double) {
    var distance: Double
    var direction: Vec2 = direction
        set(value) {
            field = value.minus(startPoint)
            distance = direction.length()
            direction.normalize()
        }
    val intersectingBodiesInfo = ArrayList<RayInformation>()

    init {
        this.direction = direction.normalized
        this.distance = distance
    }

    /**
     * Updates the projection in world space and acquires information about the closest intersecting object with the ray projection.
     *
     * @param bodiesToEvaluate Arraylist of bodies to check if they intersect with the ray projection.
     */
    fun updateProjection(bodiesToEvaluate: ArrayList<TranslatableBody>) {
        intersectingBodiesInfo.clear()
        val endPoint = direction.scalar(distance)
        val endX = endPoint.x
        val endY = endPoint.y
        var minPx: Double
        var minPy: Double
        var noOfIntersections = 0
        for (body in bodiesToEvaluate) {
            if (body !is CollisionBodyInterface) continue
            if (body.shape is Polygon) {
                val poly = body.shape as Polygon
                for (i in poly.vertices.indices) {
                    var startOfPolyEdge = poly.vertices[i]
                    var endOfPolyEdge = poly.vertices[if (i + 1 == poly.vertices.size) 0 else i + 1]
                    startOfPolyEdge = poly.orientation.mul(startOfPolyEdge, Vec2()).plus(body.position)
                    endOfPolyEdge = poly.orientation.mul(endOfPolyEdge, Vec2()).plus(body.position)
                    val dx = endOfPolyEdge.x - startOfPolyEdge.x
                    val dy = endOfPolyEdge.y - startOfPolyEdge.y

                    //Check to see if the lines are not parallel
                    if (dx - endX != 0.0 && dy - endY != 0.0) {
                        val t2 =
                            (endX * (startOfPolyEdge.y - startPoint.y) + endY * (startPoint.x - startOfPolyEdge.x)) / (dx * endY - dy * endX)
                        val t1 = (startOfPolyEdge.x + dx * t2 - startPoint.x) / endX
                        if (t1 > 0 && t2 >= 0 && t2 <= 1.0) {
                            val point = Vec2(startPoint.x + endX * t1, startPoint.y + endY * t1)
                            val dist = point.minus(startPoint).length()
                            if (dist < distance) {
                                minPx = point.x
                                minPy = point.y
                                intersectingBodiesInfo.add(RayInformation(body, minPx, minPy, i))
                                noOfIntersections++
                            }
                        }
                    }
                }
            } else if (body.shape is Circle) {
                val circle = body.shape as Circle
                val ray = endPoint.copy()
                val circleCenter = body.position.copy()
                val r = circle.radius
                val difInCenters = startPoint.minus(circleCenter)
                val a = ray.dot(ray)
                val b = 2 * difInCenters.dot(ray)
                val c = difInCenters.dot(difInCenters) - r * r
                var discriminant = b * b - 4 * a * c
                if (discriminant > 0) {
                    discriminant = sqrt(discriminant)
                    val t1 = (-b - discriminant) / (2 * a)
                    if (t1 in 0.0..1.0) {
                        minPx = startPoint.x + endX * t1
                        minPy = startPoint.y + endY * t1
                        intersectingBodiesInfo.add(RayInformation(body, minPx, minPy, -1))
                    }
                    val t2 = (-b + discriminant) / (2 * a)
                    if (t2 in 0.0..1.0) {
                        minPx = startPoint.x + endX * t2
                        minPy = startPoint.y + endY * t2
                        intersectingBodiesInfo.add(RayInformation(body, minPx, minPy, -1))
                    }
                }
            }
            if (noOfIntersections % 2 == 1) {
                intersectingBodiesInfo.removeAt(intersectingBodiesInfo.size - 1)
                noOfIntersections = 0
            }
        }
    }

    /**
     * Slices any polygons in the world supplied that intersect with the ray projection.
     *
     * @param world World object for the slice to effect.
     */
    fun sliceObjects(world: World) {
        val k = intersectingBodiesInfo.size % 2
        var i = 0
        while (i < intersectingBodiesInfo.size - k) {
            val b = intersectingBodiesInfo[i].b
            if (b !is CollisionBodyInterface || b !is PhysicalBodyInterface) continue
            val isStatic = b.mass == 0.0
            if (b.shape is Polygon) {
                val p = b.shape as Polygon
                val intersection1 = intersectingBodiesInfo[i]
                val intersection2 = intersectingBodiesInfo[i + 1]
                var obj1firstIndex = intersection1.index
                val secondIndex = intersection2.index
                val obj2firstIndex = obj1firstIndex
                var totalVerticesObj1 = obj1firstIndex + 2 + (p.vertices.size - secondIndex)
                val obj1Vertz = MutableList(totalVerticesObj1) { Vec2() }
                for (x in 0 until obj1firstIndex + 1) {
                    obj1Vertz[x] = b.shape.orientation.mul(p.vertices[x], Vec2()).plus(b.position)
                }
                obj1Vertz[++obj1firstIndex] = intersectingBodiesInfo[i].coordinates
                obj1Vertz[++obj1firstIndex] = intersectingBodiesInfo[i + 1].coordinates
                for (x in secondIndex + 1 until p.vertices.size) {
                    obj1Vertz[++obj1firstIndex] = b.shape.orientation.mul(p.vertices[x], Vec2()).plus(b.position)
                }
                var polyCentre = findPolyCentre(obj1Vertz)
                val b1 = Body(Polygon(obj1Vertz.toList().toTypedArray()), polyCentre.x, polyCentre.y)
                if (isStatic) b1.density = .0
                world.addBody(b1)
                totalVerticesObj1 = secondIndex - obj2firstIndex + 2
                val obj2Vertz = MutableList(totalVerticesObj1) { Vec2() }
                var indexToAddTo = 0
                obj2Vertz[indexToAddTo++] = intersection1.coordinates
                for (x in obj2firstIndex + 1..secondIndex) {
                    obj2Vertz[indexToAddTo++] = b.shape.orientation.mul(p.vertices[x], Vec2()).plus(b.position)
                }
                obj2Vertz[totalVerticesObj1 - 1] = intersection2.coordinates
                polyCentre = findPolyCentre(obj2Vertz)
                val b2 = Body(Polygon(obj2Vertz.toList().toTypedArray()), polyCentre.x, polyCentre.y)
                if (isStatic) b2.density = .0
                world.addBody(b2)
            }
            world.removeBody(b)
            i += 2
        }
    }

    /**
     * Finds the center of mass of a polygon and return its.
     *
     * @param obj2Vertz Vertices of polygon to find center of mass of.
     * @return Center of mass of type Vec2.
     */
    private fun findPolyCentre(obj2Vertz: MutableList<Vec2>): Vec2 {
        var accumulatedArea = 0.0
        var centerX = 0.0
        var centerY = 0.0
        var i = 0
        var j = obj2Vertz.size - 1
        while (i < obj2Vertz.size) {
            val temp = obj2Vertz[i].x * obj2Vertz[j].y - obj2Vertz[j].x * obj2Vertz[i].y
            accumulatedArea += temp
            centerX += (obj2Vertz[i].x + obj2Vertz[j].x) * temp
            centerY += (obj2Vertz[i].y + obj2Vertz[j].y) * temp
            j = i++
        }
        if (accumulatedArea == 0.0) return Vec2()
        accumulatedArea *= 3.0
        return Vec2(centerX / accumulatedArea, centerY / accumulatedArea)
    }
}