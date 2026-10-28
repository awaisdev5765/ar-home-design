package com.arhomedesign.design.data

import com.arhomedesign.core.model.MaterialCatalogItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MaterialCatalogRepository(
    private val firestore: FirebaseFirestore
) {
    suspend fun loadMaterials(): List<MaterialCatalogItem> {
        val snap = firestore.collection("materialCatalog").get().await()
        if (snap.isEmpty) {
            return listOf(
                MaterialCatalogItem(id = "default_paint", name = "Default Paint", category = "paint", presetCode = "default_paint", colorHex = "#FFFFFF"),
                MaterialCatalogItem(id = "oak_wood", name = "Oak Wood", category = "wood", presetCode = "oak_wood", colorHex = "#A67C52"),
                MaterialCatalogItem(id = "marble_tile", name = "Marble Tile", category = "tile", presetCode = "marble_tile", colorHex = "#E5E5E5")
            )
        }
        return snap.documents.map { doc ->
            MaterialCatalogItem(
                id = doc.id,
                name = doc.getString("name") ?: doc.id,
                category = doc.getString("category") ?: "general",
                presetCode = doc.getString("presetCode") ?: doc.id,
                colorHex = doc.getString("colorHex") ?: "#FFFFFF",
                active = doc.getBoolean("active") ?: true
            )
        }.filter { it.active }
    }
}
