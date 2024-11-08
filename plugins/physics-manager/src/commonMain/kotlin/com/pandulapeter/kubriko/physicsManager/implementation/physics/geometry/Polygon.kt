package com.pandulapeter.kubriko.physicsManager.implementation.physics.geometry

import com.pandulapeter.kubriko.physicsManager.implementation.physics.collision.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.physicsManager.implementation.physics.dynamics.bodies.PhysicalBodyInterface
import com.pandulapeter.kubriko.physicsManager.implementation.physics.geometry.bodies.TranslatableBody
import com.pandulapeter.kubriko.physicsManager.implementation.physics.math.Math.lineIntersect
import com.pandulapeter.kubriko.physicsManager.implementation.physics.math.Math.pointIsOnLine
import com.pandulapeter.kubriko.physicsManager.implementation.physics.math.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Class for representing polygon shape.
 */
class Polygon : Shape {
    var vertices: Array<Vec2>
    lateinit var normals: Array<Vec2>

    /**
     * Constructor takes a supplied list of vertices and generates a convex hull around them.
     *
     * @param vertList Vertices of polygon to create.
     */
    constructor(vertList: Array<Vec2>) {
        vertices = generateHull(vertList, vertList.size)
        calcNormals()
    }

    /**
     * Constructor to generate a rectangle.
     *
     * @param halfWidth  Desired width of rectangle
     * @param halfHeight Desired height of rectangle
     */
    constructor(halfWidth: Double, halfHeight: Double) {
        vertices = arrayOf(
            Vec2(-halfWidth, -halfHeight),
            Vec2(halfWidth, -halfHeight),
            Vec2(halfWidth, halfHeight),
            Vec2(-halfWidth, halfHeight)
        )
        normals = arrayOf(
            Vec2(0.0, -1.0),
            Vec2(1.0, 0.0),
            Vec2(0.0, 1.0),
            Vec2(-1.0, 0.0)
        )
    }

    /**
     * Generate a regular polygon with a specified number of sides and size.
     *
     * @param radius    The maximum distance any vertex is away from the center of mass.
     * @param noOfSides The desired number of face the polygon has.
     */
    constructor(radius: Int, noOfSides: Int) {
        val vertices = MutableList(noOfSides) { Vec2() }
        for (i in 0 until noOfSides) {
            val angle = 2 * PI / noOfSides * (i + 0.75)
            val pointX = radius * cos(angle)
            val pointY = radius * sin(angle)
            vertices[i] = Vec2(pointX, pointY)
        }
        this.vertices = vertices.toList().toTypedArray()
        calcNormals()
    }

    /**
     * Generates normals for each face of the polygon. Positive normals of polygon faces face outward.
     */
    private fun calcNormals() {
        val normals = MutableList(vertices.size) { Vec2() }
        for (i in vertices.indices) {
            val face = vertices[if (i + 1 == vertices.size) 0 else i + 1].minus(vertices[i])
            normals[i] = face.normal().normalize().unaryMinus()
        }
        this.normals = normals.toList().toTypedArray()
    }

    /**
     * Implementation of calculating the mass of a polygon.
     *
     * @param density The desired density to factor into the calculation.
     */
    override fun calcMass(density: Double) {
        val physicalBody = this.body
        if (physicalBody !is PhysicalBodyInterface) return
        var centroidDistVec: Vec2? =
            Vec2(0.0, 0.0)
        var area = 0.0
        var inertia = 0.0
        val k = 1.0 / 3.0
        for (i in vertices.indices) {
            val point1 = vertices[i]
            val point2 = vertices[(i + 1) % vertices.size]
            val areaOfParallelogram = point1.cross(point2)
            val triangleArea = 0.5 * areaOfParallelogram
            area += triangleArea
            val weight = triangleArea * k
            centroidDistVec!!.add(point1.scalar(weight))
            centroidDistVec.add(point2.scalar(weight))
            val intx2 = point1.x * point1.x + point2.x * point1.x + point2.x * point2.x
            val inty2 = point1.y * point1.y + point2.y * point1.y + point2.y * point2.y
            inertia += 0.25 * k * areaOfParallelogram * (intx2 + inty2)
        }
        centroidDistVec = centroidDistVec!!.scalar(1.0 / area)
        for (i in vertices.indices) {
            vertices[i] = vertices[i].minus(centroidDistVec)
        }
        physicalBody.mass = density * area
        physicalBody.invMass = if (physicalBody.mass != 0.0) 1.0 / physicalBody.mass else 0.0
        physicalBody.inertia = inertia * density
        physicalBody.invInertia = if (physicalBody.inertia != 0.0) 1.0 / physicalBody.inertia else 0.0
    }

    /**
     * Generates an AABB encompassing the polygon and binds it to the body.
     */
    override fun createAABB() {
        val firstPoint = orientation.mul(vertices[0], Vec2())
        var minX = firstPoint.x
        var maxX = firstPoint.x
        var minY = firstPoint.y
        var maxY = firstPoint.y
        for (i in 1 until vertices.size) {
            val point = orientation.mul(vertices[i], Vec2())
            val px = point.x
            val py = point.y
            if (px < minX) {
                minX = px
            } else if (px > maxX) {
                maxX = px
            }
            if (py < minY) {
                minY = py
            } else if (py > maxY) {
                maxY = py
            }
        }
        this.body.aabb = AxisAlignedBoundingBox(
            Vec2(minX, minY),
            Vec2(maxX, maxY)
        )
    }

    /**
     * Generates a convex hull around the vertices supplied.
     *
     * @param vertices List of vertices.
     * @param n        Number of vertices supplied.
     * @return Returns a convex hull array.
     */
    private fun generateHull(vertices: Array<Vec2>, n: Int): Array<Vec2> {
        val hull = ArrayList<Vec2>()
        var firstPointIndex = 0
        var minX = Double.MAX_VALUE
        for (i in 0 until n) {
            val x = vertices[i].x
            if (x < minX) {
                firstPointIndex = i
                minX = x
            }
        }
        var point = firstPointIndex
        var currentEvalPoint: Int
        var first = true
        while (point != firstPointIndex || first) {
            first = false
            hull.add(vertices[point])
            currentEvalPoint = (point + 1) % n
            for (i in 0 until n) {
                if (sideOfLine(vertices[point], vertices[i], vertices[currentEvalPoint]) == -1) currentEvalPoint = i
            }
            point = currentEvalPoint
        }
        val hulls = MutableList(hull.size) { Vec2() }
        for (i in hull.indices) {
            hulls[i] = hull[i]
        }
        return hulls.toList().toTypedArray()
    }

    /**
     * Checks which side of a line a point is on.
     *
     * @param p1    Vertex of line to evaluate.
     * @param p2    Vertex of line to evaluate.
     * @param point Point to check which side it lies on.
     * @return Int value - positive = right side of line. Negative = left side of line.
     */
    private fun sideOfLine(p1: Vec2, p2: Vec2, point: Vec2): Int {
        val value = (p2.y - p1.y) * (point.x - p2.x) - (p2.x - p1.x) * (point.y - p2.y)
        return if (value > 0) 1 else if (value == 0.0) 0 else -1
    }

    /**
     * Method to check if point is inside a body in world space.
     *
     * @param startPoint Vector point to check if its inside the first body.
     * @return boolean value whether the point is inside the first body.
     */
    override fun isPointInside(startPoint: Vec2): Boolean {
        for (i in vertices.indices) {
            val objectPoint = startPoint.minus(
                this.body.position.plus(
                    this.body.shape.orientation.mul(
                        vertices[i],
                        Vec2()
                    )
                )
            )
            if (objectPoint.dot(this.body.shape.orientation.mul(normals[i], Vec2())) > 0) {
                return false
            }
        }
        return true
    }

    override fun rayIntersect(startPoint: Vec2, endPoint: Vec2, maxDistance: Double, rayLength: Double): IntersectionReturnElement {
        var minPx = 0.0
        var minPy = 0.0
        var intersectionFound = false
        var closestBody: TranslatableBody? = null
        var maxD = maxDistance

        for (i in vertices.indices) {
            var startOfPolyEdge = vertices[i]
            var endOfPolyEdge = vertices[if (i + 1 == vertices.size) 0 else i + 1]
            startOfPolyEdge = orientation.mul(startOfPolyEdge, Vec2()).plus(body.position)
            endOfPolyEdge = orientation.mul(endOfPolyEdge, Vec2()).plus(body.position)

            //detect if line (startPoint -> endpoint) intersects with the current edge (startOfPolyEdge -> endOfPolyEdge)
            val intersection = lineIntersect(startPoint, endPoint, startOfPolyEdge, endOfPolyEdge)
            if (intersection != null) {
                val distance = startPoint.distance(intersection)
                if (pointIsOnLine(startPoint, endPoint, intersection) && pointIsOnLine(startOfPolyEdge, endOfPolyEdge, intersection) && distance < maxD) {
                    maxD = distance
                    minPx = intersection.x
                    minPy = intersection.y
                    intersectionFound = true
                    closestBody = body
                }
            }
        }
        return IntersectionReturnElement(minPx, minPy, intersectionFound, closestBody, maxD)
    }
}