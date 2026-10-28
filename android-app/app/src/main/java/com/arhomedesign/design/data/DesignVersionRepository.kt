package com.arhomedesign.design.data

import com.arhomedesign.core.model.DesignVersion
import com.arhomedesign.core.model.OverlayRegion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DesignVersionRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun saveVersion(
        projectId: String,
        roomId: String,
        notes: String,
        overlays: List<OverlayRegion>
    ): String {
        val uid = auth.currentUser?.uid ?: error("User must be authenticated")
        val existing = firestore.collection("designVersions")
            .whereEqualTo("ownerId", uid)
            .whereEqualTo("projectId", projectId)
            .whereEqualTo("roomId", roomId)
            .get()
            .await()
        val nextVersion = existing.size() + 1
        val doc = firestore.collection("designVersions").document()
        doc.set(
            mapOf(
                "projectId" to projectId,
                "roomId" to roomId,
                "ownerId" to uid,
                "versionNumber" to nextVersion,
                "notes" to notes,
                "overlayCount" to overlays.size,
                "overlaySnapshot" to overlays.map {
                    mapOf(
                        "id" to it.id,
                        "surfaceType" to it.surfaceType,
                        "materialPreset" to it.materialPreset,
                        "colorHex" to it.colorHex
                    )
                },
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        return doc.id
    }

    suspend fun loadVersions(projectId: String, roomId: String?): List<DesignVersion> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        var query = firestore.collection("designVersions")
            .whereEqualTo("ownerId", uid)
            .whereEqualTo("projectId", projectId)
        if (!roomId.isNullOrBlank()) query = query.whereEqualTo("roomId", roomId)
        val snap = query.get().await()
        return snap.documents.map { doc ->
            DesignVersion(
                id = doc.id,
                projectId = doc.getString("projectId") ?: "",
                roomId = doc.getString("roomId") ?: "",
                ownerId = doc.getString("ownerId") ?: "",
                versionNumber = (doc.getLong("versionNumber") ?: 1L).toInt(),
                notes = doc.getString("notes") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L
            )
        }.sortedByDescending { it.versionNumber }
    }
}
