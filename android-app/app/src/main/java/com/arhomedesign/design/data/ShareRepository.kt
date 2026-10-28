package com.arhomedesign.design.data

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class ShareRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    suspend fun createShareLink(projectId: String, versionId: String?): String {
        val result = functions
            .getHttpsCallable("createShareLink")
            .call(
                mapOf(
                    "projectId" to projectId,
                    "versionId" to (versionId ?: ""),
                    "expiresInDays" to 7
                )
            )
            .await()
        val data = result.getData() as? Map<*, *> ?: return "Share link generated."
        return data["shareUrl"]?.toString() ?: "Share link generated."
    }

    suspend fun buildSharePackage(projectId: String, roomId: String?): String {
        val result = functions
            .getHttpsCallable("buildSharePackage")
            .call(
                mapOf(
                    "projectId" to projectId,
                    "roomId" to (roomId ?: "")
                )
            )
            .await()
        val data = result.getData() as? Map<*, *> ?: return "Share package generated."
        val overlayCount = data["overlayCount"]?.toString() ?: "0"
        val estimate = (data["latestEstimate"] as? Map<*, *>)?.get("total")?.toString() ?: "N/A"
        return "Share package ready. Overlays: $overlayCount, Latest estimate total: $estimate"
    }
}
