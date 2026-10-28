package com.arhomedesign.design.data

import com.arhomedesign.core.model.OverlayRegion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class OverlayRegionRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun addOverlay(
        projectId: String,
        roomId: String,
        name: String,
        surfaceType: String,
        materialPreset: String,
        colorHex: String
    ): String {
        val uid = auth.currentUser?.uid ?: error("User must be authenticated")
        val ref = firestore.collection("overlayRegions").document()
        ref.set(
            mapOf(
                "projectId" to projectId,
                "roomId" to roomId,
                "ownerId" to uid,
                "name" to name,
                "surfaceType" to surfaceType,
                "materialPreset" to materialPreset,
                "colorHex" to colorHex,
                "positionX" to 0.0,
                "positionY" to 0.0,
                "positionZ" to 0.0,
                "widthMeters" to 1.0,
                "heightMeters" to 1.0,
                "rotationDeg" to 0.0,
                "status" to "active"
            )
        ).await()
        return ref.id
    }

    suspend fun loadOverlays(projectId: String, roomId: String?): List<OverlayRegion> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        var query = firestore.collection("overlayRegions")
            .whereEqualTo("ownerId", uid)
            .whereEqualTo("projectId", projectId)
        if (!roomId.isNullOrBlank()) {
            query = query.whereEqualTo("roomId", roomId)
        }
        val snap = query.get().await()

        return snap.documents.map { doc ->
            OverlayRegion(
                id = doc.id,
                projectId = doc.getString("projectId") ?: "",
                roomId = doc.getString("roomId") ?: "",
                ownerId = doc.getString("ownerId") ?: "",
                name = doc.getString("name") ?: "",
                surfaceType = doc.getString("surfaceType") ?: "wall",
                materialPreset = doc.getString("materialPreset") ?: "default_paint",
                colorHex = doc.getString("colorHex") ?: "#FFFFFF",
                positionX = doc.getDouble("positionX") ?: 0.0,
                positionY = doc.getDouble("positionY") ?: 0.0,
                positionZ = doc.getDouble("positionZ") ?: 0.0,
                widthMeters = doc.getDouble("widthMeters") ?: 1.0,
                heightMeters = doc.getDouble("heightMeters") ?: 1.0,
                rotationDeg = doc.getDouble("rotationDeg") ?: 0.0,
                status = doc.getString("status") ?: "active"
            )
        }
    }

    suspend fun updateOverlayMaterial(
        overlayId: String,
        materialPreset: String,
        colorHex: String
    ) {
        val uid = auth.currentUser?.uid ?: error("User must be authenticated")
        val doc = firestore.collection("overlayRegions").document(overlayId)
        val snap = doc.get().await()
        if (snap.getString("ownerId") != uid) error("Overlay ownership mismatch")
        doc.update(
            mapOf(
                "materialPreset" to materialPreset,
                "colorHex" to colorHex
            )
        ).await()
    }

    suspend fun updateOverlayTransform(
        overlayId: String,
        positionX: Double,
        positionY: Double,
        positionZ: Double,
        widthMeters: Double,
        heightMeters: Double,
        rotationDeg: Double
    ) {
        val uid = auth.currentUser?.uid ?: error("User must be authenticated")
        val doc = firestore.collection("overlayRegions").document(overlayId)
        val snap = doc.get().await()
        if (snap.getString("ownerId") != uid) error("Overlay ownership mismatch")
        doc.update(
            mapOf(
                "positionX" to positionX,
                "positionY" to positionY,
                "positionZ" to positionZ,
                "widthMeters" to widthMeters,
                "heightMeters" to heightMeters,
                "rotationDeg" to rotationDeg
            )
        ).await()
    }
}
