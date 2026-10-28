import Foundation
import FirebaseAuth
import FirebaseFirestore

final class OverlayRegionService {
    private let db = Firestore.firestore()

    func addOverlay(
        projectId: String,
        roomId: String,
        name: String,
        surfaceType: String,
        materialPreset: String,
        colorHex: String
    ) async throws -> String {
        guard let uid = Auth.auth().currentUser?.uid else {
            throw NSError(domain: "OverlayRegionService", code: 401, userInfo: [
                NSLocalizedDescriptionKey: "User must be authenticated."
            ])
        }

        let ref = db.collection("overlayRegions").document()
        try await ref.setData([
            "projectId": projectId,
            "roomId": roomId,
            "ownerId": uid,
            "name": name,
            "surfaceType": surfaceType,
            "materialPreset": materialPreset,
            "colorHex": colorHex,
            "positionX": 0.0,
            "positionY": 0.0,
            "positionZ": 0.0,
            "widthMeters": 1.0,
            "heightMeters": 1.0,
            "rotationDeg": 0.0,
            "status": "active"
        ])
        return ref.documentID
    }

    func loadOverlays(projectId: String, roomId: String?) async throws -> [OverlayRegion] {
        guard let uid = Auth.auth().currentUser?.uid else { return [] }
        var query = db.collection("overlayRegions")
            .whereField("ownerId", isEqualTo: uid)
            .whereField("projectId", isEqualTo: projectId)
        if let roomId, !roomId.isEmpty {
            query = query.whereField("roomId", isEqualTo: roomId)
        }
        let snapshot = try await query.getDocuments()
        return snapshot.documents.map { doc in
            let data = doc.data()
            return OverlayRegion(
                id: doc.documentID,
                projectId: data["projectId"] as? String ?? "",
                roomId: data["roomId"] as? String ?? "",
                ownerId: data["ownerId"] as? String ?? "",
                name: data["name"] as? String ?? "",
                surfaceType: data["surfaceType"] as? String ?? "wall",
                materialPreset: data["materialPreset"] as? String ?? "default_paint",
                colorHex: data["colorHex"] as? String ?? "#FFFFFF",
                positionX: data["positionX"] as? Double ?? 0.0,
                positionY: data["positionY"] as? Double ?? 0.0,
                positionZ: data["positionZ"] as? Double ?? 0.0,
                widthMeters: data["widthMeters"] as? Double ?? 1.0,
                heightMeters: data["heightMeters"] as? Double ?? 1.0,
                rotationDeg: data["rotationDeg"] as? Double ?? 0.0,
                status: data["status"] as? String ?? "active"
            )
        }
    }

    func updateOverlayMaterial(overlayId: String, materialPreset: String, colorHex: String) async throws {
        guard let uid = Auth.auth().currentUser?.uid else {
            throw NSError(domain: "OverlayRegionService", code: 401, userInfo: [
                NSLocalizedDescriptionKey: "User must be authenticated."
            ])
        }
        let ref = db.collection("overlayRegions").document(overlayId)
        let snap = try await ref.getDocument()
        if snap.data()?["ownerId"] as? String != uid {
            throw NSError(domain: "OverlayRegionService", code: 403, userInfo: [
                NSLocalizedDescriptionKey: "Overlay ownership mismatch."
            ])
        }
        try await ref.updateData([
            "materialPreset": materialPreset,
            "colorHex": colorHex
        ])
    }

    func updateOverlayTransform(
        overlayId: String,
        positionX: Double,
        positionY: Double,
        positionZ: Double,
        widthMeters: Double,
        heightMeters: Double,
        rotationDeg: Double
    ) async throws {
        guard let uid = Auth.auth().currentUser?.uid else {
            throw NSError(domain: "OverlayRegionService", code: 401, userInfo: [
                NSLocalizedDescriptionKey: "User must be authenticated."
            ])
        }
        let ref = db.collection("overlayRegions").document(overlayId)
        let snap = try await ref.getDocument()
        if snap.data()?["ownerId"] as? String != uid {
            throw NSError(domain: "OverlayRegionService", code: 403, userInfo: [
                NSLocalizedDescriptionKey: "Overlay ownership mismatch."
            ])
        }
        try await ref.updateData([
            "positionX": positionX,
            "positionY": positionY,
            "positionZ": positionZ,
            "widthMeters": widthMeters,
            "heightMeters": heightMeters,
            "rotationDeg": rotationDeg
        ])
    }
}
