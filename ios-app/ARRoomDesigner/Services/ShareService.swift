import Foundation
import FirebaseFunctions

final class ShareService {
    private let functions = Functions.functions()

    func createShareLink(projectId: String, versionId: String?) async throws -> String {
        let result = try await functions.httpsCallable("createShareLink").call([
            "projectId": projectId,
            "versionId": versionId ?? "",
            "expiresInDays": 7
        ])
        let data = result.data as? [String: Any]
        return data?["shareUrl"] as? String ?? "Share link generated."
    }

    func buildSharePackage(projectId: String, roomId: String?) async throws -> String {
        let result = try await functions.httpsCallable("buildSharePackage").call([
            "projectId": projectId,
            "roomId": roomId ?? ""
        ])
        let data = result.data as? [String: Any]
        let overlayCount = data?["overlayCount"] as? Int ?? 0
        let latestEstimate = data?["latestEstimate"] as? [String: Any]
        let total = latestEstimate?["total"] as? Double ?? 0
        return "Share package ready. Overlays: \(overlayCount), Latest estimate total: \(String(format: "%.2f", total))"
    }
}
