import Foundation
import FirebaseAuth
import FirebaseFirestore

final class RoomScanService {
    private let db = Firestore.firestore()

    struct ScanCaptureMetrics {
        let planeCount: Int
        let anchorCount: Int
        let widthMeters: Double
        let lengthMeters: Double
        let heightMeters: Double
        let confidence: Double
    }

    func saveScan(projectId: String, roomName: String, metrics: ScanCaptureMetrics) async throws -> String {
        guard let uid = Auth.auth().currentUser?.uid else {
            throw NSError(domain: "RoomScanService", code: 401, userInfo: [
                NSLocalizedDescriptionKey: "User must be authenticated."
            ])
        }
        let ref = db.collection("rooms").document()
        try await ref.setData([
            "projectId": projectId,
            "ownerId": uid,
            "name": roomName.trimmingCharacters(in: .whitespacesAndNewlines),
            "scanMetadata": [
                "planeCount": metrics.planeCount,
                "anchorCount": metrics.anchorCount,
                "widthMeters": metrics.widthMeters,
                "lengthMeters": metrics.lengthMeters,
                "heightMeters": metrics.heightMeters,
                "confidence": metrics.confidence,
                "capturedAt": Date().timeIntervalSince1970
            ],
            "status": "captured"
        ])
        return ref.documentID
    }

    func loadScans(projectId: String) async throws -> [RoomScan] {
        guard let uid = Auth.auth().currentUser?.uid else { return [] }
        let query = db.collection("rooms")
            .whereField("ownerId", isEqualTo: uid)
            .whereField("projectId", isEqualTo: projectId)
        let snapshot = try await query.getDocuments()
        return snapshot.documents.map { doc in
            let data = doc.data()
            let metadata = data["scanMetadata"] as? [String: Any]
            return RoomScan(
                id: doc.documentID,
                projectId: data["projectId"] as? String ?? "",
                ownerId: data["ownerId"] as? String ?? "",
                name: data["name"] as? String ?? "",
                planeCount: metadata?["planeCount"] as? Int ?? 0,
                anchorCount: metadata?["anchorCount"] as? Int ?? 0,
                widthMeters: metadata?["widthMeters"] as? Double ?? 0.0,
                lengthMeters: metadata?["lengthMeters"] as? Double ?? 0.0,
                heightMeters: metadata?["heightMeters"] as? Double ?? 0.0,
                confidence: metadata?["confidence"] as? Double ?? 0.0,
                status: data["status"] as? String ?? "captured"
            )
        }
    }
}
