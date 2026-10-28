package com.arhomedesign.ar.data

import android.content.Context
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlin.math.max

class ARCorePlaneDetector {
    private var session: Session? = null

    data class RoomEstimate(
        val planeCount: Int,
        val anchorCount: Int,
        val widthMeters: Double,
        val lengthMeters: Double,
        val heightMeters: Double,
        val confidence: Double
    )

    fun start(context: Context): Result<Unit> = runCatching {
        if (session != null) return@runCatching
        ArCoreSupportHelper.ensureCanOpenSession(context)
        val newSession = Session(context)
        val config = Config(newSession).apply {
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        }
        newSession.configure(config)
        newSession.resume()
        session = newSession
    }

    fun currentTrackedPlaneCount(): Int {
        val activeSession = session ?: return 0
        return activeSession.getAllTrackables(Plane::class.java).count { it.trackingState == TrackingState.TRACKING }
    }

    fun currentRoomEstimate(): RoomEstimate {
        val activeSession = session ?: return RoomEstimate(0, 0, 0.0, 0.0, 0.0, 0.0)
        val trackedPlanes = activeSession.getAllTrackables(Plane::class.java)
            .filter { it.trackingState == TrackingState.TRACKING }

        var maxWidth = 0.0
        var maxLength = 0.0
        trackedPlanes.forEach { plane ->
            maxWidth = max(maxWidth, plane.extentX.toDouble())
            maxLength = max(maxLength, plane.extentZ.toDouble())
        }
        val planeCount = trackedPlanes.size
        val confidence = when {
            planeCount >= 6 -> 0.95
            planeCount >= 3 -> 0.8
            planeCount >= 1 -> 0.6
            else -> 0.0
        }
        return RoomEstimate(
            planeCount = planeCount,
            anchorCount = planeCount,
            widthMeters = maxWidth,
            lengthMeters = maxLength,
            heightMeters = 2.8,
            confidence = confidence
        )
    }

    fun stop() {
        session?.pause()
        session?.close()
        session = null
    }
}
