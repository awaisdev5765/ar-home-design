package com.arhomedesign.ar.data

import com.arhomedesign.core.model.RoomScan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RoomScanRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    data class ScanCaptureMetrics(
        val planeCount: Int,
        val anchorCount: Int,
        val widthMeters: Double,
        val lengthMeters: Double,
        val heightMeters: Double,
        val confidence: Double
    )

    suspend fun saveRoomScan(projectId: String, roomName: String, metrics: ScanCaptureMetrics): String {
        val uid = auth.currentUser?.uid ?: error("User must be authenticated")
        val ref = firestore.collection("rooms").document()
        val payload = mapOf(
            "projectId" to projectId,
            "ownerId" to uid,
            "name" to roomName.trim(),
            "scanMetadata" to mapOf(
                "planeCount" to metrics.planeCount,
                "anchorCount" to metrics.anchorCount,
                "widthMeters" to metrics.widthMeters,
                "lengthMeters" to metrics.lengthMeters,
                "heightMeters" to metrics.heightMeters,
                "confidence" to metrics.confidence,
                "capturedAt" to System.currentTimeMillis()
            ),
            "status" to "captured"
        )
        ref.set(payload).await()
        return ref.id
    }

    suspend fun loadRoomScans(projectId: String): List<RoomScan> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snap = firestore.collection("rooms")
            .whereEqualTo("ownerId", uid)
            .whereEqualTo("projectId", projectId)
            .get()
            .await()

        return snap.documents.map { doc ->
            val metadata = doc.get("scanMetadata") as? Map<*, *>
            RoomScan(
                id = doc.id,
                projectId = doc.getString("projectId") ?: "",
                ownerId = doc.getString("ownerId") ?: "",
                name = doc.getString("name") ?: "",
                planeCount = (metadata?.get("planeCount") as? Number)?.toInt() ?: 0,
                anchorCount = (metadata?.get("anchorCount") as? Number)?.toInt() ?: 0,
                widthMeters = (metadata?.get("widthMeters") as? Number)?.toDouble() ?: 0.0,
                lengthMeters = (metadata?.get("lengthMeters") as? Number)?.toDouble() ?: 0.0,
                heightMeters = (metadata?.get("heightMeters") as? Number)?.toDouble() ?: 0.0,
                confidence = (metadata?.get("confidence") as? Number)?.toDouble() ?: 0.0,
                status = doc.getString("status") ?: "captured"
            )
        }
    }
}
