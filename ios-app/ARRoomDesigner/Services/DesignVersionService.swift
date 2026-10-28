import Foundation
import FirebaseAuth
import FirebaseFirestore

final class DesignVersionService {
    private let db = Firestore.firestore()

    func saveVersion(projectId: String, roomId: String, notes: String, overlays: [OverlayRegion]) async throws -> String {
        guard let uid = Auth.auth().currentUser?.uid else {
            throw NSError(domain: "DesignVersionService", code: 401, userInfo: [
                NSLocalizedDescriptionKey: "User must be authenticated."
            ])
        }
        let existing = try await db.collection("designVersions")
            .whereField("ownerId", isEqualTo: uid)
            .whereField("projectId", isEqualTo: projectId)
            .whereField("roomId", isEqualTo: roomId)
            .getDocuments()
        let nextVersion = existing.documents.count + 1
        let ref = db.collection("designVersions").document()
        try await ref.setData([
            "projectId": projectId,
            "roomId": roomId,
            "ownerId": uid,
            "versionNumber": nextVersion,
            "notes": notes,
            "overlayCount": overlays.count,
            "overlaySnapshot": overlays.map { [
                "id": $0.id,
                "surfaceType": $0.surfaceType,
                "materialPreset": $0.materialPreset,
                "colorHex": $0.colorHex
            ] },
            "createdAt": Date().timeIntervalSince1970
        ])
        return ref.documentID
    }

    func loadVersions(projectId: String, roomId: String?) async throws -> [DesignVersion] {
        guard let uid = Auth.auth().currentUser?.uid else { return [] }
        var query = db.collection("designVersions")
            .whereField("ownerId", isEqualTo: uid)
            .whereField("projectId", isEqualTo: projectId)
        if let roomId, !roomId.isEmpty {
            query = query.whereField("roomId", isEqualTo: roomId)
        }
        let snapshot = try await query.getDocuments()
        return snapshot.documents.map { doc in
            let data = doc.data()
            return DesignVersion(
                id: doc.documentID,
                projectId: data["projectId"] as? String ?? "",
                roomId: data["roomId"] as? String ?? "",
                ownerId: data["ownerId"] as? String ?? "",
                versionNumber: data["versionNumber"] as? Int ?? 1,
                notes: data["notes"] as? String ?? "",
                createdAt: data["createdAt"] as? TimeInterval ?? 0
            )
        }.sorted { $0.versionNumber > $1.versionNumber }
    }
}
