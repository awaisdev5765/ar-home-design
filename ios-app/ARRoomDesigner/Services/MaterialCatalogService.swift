import Foundation
import FirebaseFirestore

final class MaterialCatalogService {
    private let db = Firestore.firestore()

    func loadMaterials() async throws -> [MaterialCatalogItem] {
        let snapshot = try await db.collection("materialCatalog").getDocuments()
        if snapshot.documents.isEmpty {
            return [
                MaterialCatalogItem(id: "default_paint", name: "Default Paint", category: "paint", presetCode: "default_paint", colorHex: "#FFFFFF", active: true),
                MaterialCatalogItem(id: "oak_wood", name: "Oak Wood", category: "wood", presetCode: "oak_wood", colorHex: "#A67C52", active: true),
                MaterialCatalogItem(id: "marble_tile", name: "Marble Tile", category: "tile", presetCode: "marble_tile", colorHex: "#E5E5E5", active: true)
            ]
        }
        return snapshot.documents.map { doc in
            let data = doc.data()
            return MaterialCatalogItem(
                id: doc.documentID,
                name: data["name"] as? String ?? doc.documentID,
                category: data["category"] as? String ?? "general",
                presetCode: data["presetCode"] as? String ?? doc.documentID,
                colorHex: data["colorHex"] as? String ?? "#FFFFFF",
                active: data["active"] as? Bool ?? true
            )
        }.filter { $0.active }
    }
}
