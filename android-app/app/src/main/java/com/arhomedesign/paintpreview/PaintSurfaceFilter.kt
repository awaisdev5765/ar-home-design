package com.arhomedesign.paintpreview

import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import kotlin.math.abs

/** TRACKING is best; PAUSED still shows tint while ARCore catches up (common while moving the phone). */
fun Plane.isDrawableForPaintPreview(): Boolean =
    trackingState == TrackingState.TRACKING || trackingState == TrackingState.PAUSED

/**
 * Which detected planes receive the live paint tint.
 *
 * Classification uses [Plane.Type] plus the plane **normal in world space** from [com.google.ar.core.Pose.getYAxis]
 * (outward normal). That separates **walls** (normal ≈ horizontal) from **floors** (normal up) and **ceilings** (normal down)
 * more reliably than type alone—similar idea to consumer AR paint apps, still without full ML segmentation.
 */
enum class PaintSurfaceFilter(val label: String, val description: String) {
    ALL("All", "Every tracked plane — use while scanning"),
    WALL(
        "Wall",
        "Doors, drywall, cabinets: anything mostly upright. Move phone slowly along the wall; " +
            "raise Wall sensitivity (slider) if you still see 0 matches.",
    ),
    FLOOR("Floor", "Ground & large decks"),
    CEILING("Ceiling", "Overhead horizontal only"),
    ACCENT("Tables & decor", "Smaller upward surfaces — counters, tables"),
}

/**
 * @param wallMaxAbsNormalY for **Wall** filter: max allowed \|world Y component of the plane outward normal\|.
 *        Upright walls are ~0.05–0.35. **Higher value = more planes count as walls** (slider to the right).
 */
fun Plane.matchesFilter(
    filter: PaintSurfaceFilter,
    wallMaxAbsNormalY: Float = 0.50f,
): Boolean {
    if (subsumedBy != null) return false
    val ny = centerPose.yAxis[1]
    return when (filter) {
        PaintSurfaceFilter.ALL -> true
        PaintSurfaceFilter.WALL -> isWallSurface(ny, wallMaxAbsNormalY)
        PaintSurfaceFilter.FLOOR -> isFloorSurface(ny)
        PaintSurfaceFilter.CEILING -> isCeilingSurface(ny)
        PaintSurfaceFilter.ACCENT -> isAccentSurface(ny)
    }
}

private fun Plane.isCeilingSurface(ny: Float): Boolean {
    if (type == Plane.Type.HORIZONTAL_DOWNWARD_FACING) return true
    return ny < -0.40f
}

private fun Plane.isFloorSurface(ny: Float): Boolean {
    when (type) {
        Plane.Type.HORIZONTAL_UPWARD_FACING -> return isLikelyFloor(this)
        Plane.Type.HORIZONTAL_DOWNWARD_FACING -> return false
        Plane.Type.VERTICAL -> return false
    }
    return ny > 0.52f && isLikelyFloor(this)
}

private fun Plane.isAccentSurface(ny: Float): Boolean {
    if (type == Plane.Type.HORIZONTAL_DOWNWARD_FACING) return false
    if (type != Plane.Type.HORIZONTAL_UPWARD_FACING) return false
    if (isLikelyFloor(this)) return false
    return ny > 0.20f
}

private fun Plane.isWallSurface(ny: Float, maxAbsNy: Float): Boolean {
    if (type == Plane.Type.HORIZONTAL_DOWNWARD_FACING) return false
    if (type == Plane.Type.HORIZONTAL_UPWARD_FACING && isLikelyFloor(this)) return false

    // Ceiling-ish: outward normal points strongly toward world -Y
    if (ny <= -0.40f) return false
    // Floor / flat tabletop facing up: normal ≈ world +Y
    if (ny >= 0.72f) return false

    val cap = maxAbsNy.coerceIn(0.22f, 0.62f)

    return when (type) {
        Plane.Type.VERTICAL ->
            // ARCore vertical meshes: allow strong tilt; hard rejects are floor/ceiling only
            abs(ny) <= cap + 0.30f
        Plane.Type.HORIZONTAL_UPWARD_FACING ->
            // Shallow tilted panels sometimes classified horizontal — keep tighter than vertical
            abs(ny) <= cap + 0.14f
        Plane.Type.HORIZONTAL_DOWNWARD_FACING ->
            false
    }
}

private fun isLikelyFloor(plane: Plane): Boolean {
    val a = plane.extentX * plane.extentZ
    // Slightly stricter area so small floor patches don’t steal every horizontal classification
    return a > 3.2f || plane.extentX > 2.4f || plane.extentZ > 2.4f
}

/** Stable bucket for per-surface paint (studio / production-style multi-surface tint). */
enum class SurfacePaintKind {
    WALL,
    FLOOR,
    CEILING,
    ACCENT,
    UNKNOWN,
}

/**
 * Classifies a [Plane] for independent wall / floor / ceiling colors.
 * Order avoids double-labeling (ceiling before floor before wall).
 */
fun Plane.surfacePaintKind(wallMaxAbsNormalY: Float = 0.50f): SurfacePaintKind {
    if (subsumedBy != null) return SurfacePaintKind.UNKNOWN
    val ny = centerPose.yAxis[1]
    if (isCeilingSurface(ny)) return SurfacePaintKind.CEILING
    if (isFloorSurface(ny)) return SurfacePaintKind.FLOOR
    if (isWallSurface(ny, wallMaxAbsNormalY)) return SurfacePaintKind.WALL
    if (isAccentSurface(ny)) return SurfacePaintKind.ACCENT
    return SurfacePaintKind.UNKNOWN
}
