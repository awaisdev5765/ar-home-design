package com.arhomedesign.design.data

import com.arhomedesign.core.model.OverlayRegion
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class EstimateRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    suspend fun generateEstimate(
        projectId: String,
        roomId: String?,
        overlays: List<OverlayRegion>,
        region: String = "UAE"
    ): String {
        val result = functions
            .getHttpsCallable("generateEstimate")
            .call(
                mapOf(
                    "projectId" to projectId,
                    "roomId" to (roomId ?: ""),
                    "overlayCount" to overlays.size,
                    "overlays" to overlays.map {
                        mapOf(
                            "materialPreset" to it.materialPreset,
                            "widthMeters" to it.widthMeters,
                            "heightMeters" to it.heightMeters
                        )
                    },
                    "region" to region
                )
            )
            .await()

        val data = result.getData() as? Map<*, *> ?: return "Estimate generated."
        val total = data["total"]?.toString() ?: "0"
        val currency = data["currency"]?.toString() ?: ""
        return "Estimate: $total $currency"
    }
}
