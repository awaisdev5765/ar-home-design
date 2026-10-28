import Foundation
import FirebaseFunctions

final class EstimateService {
    private let functions = Functions.functions()

    func generateEstimate(projectId: String, roomId: String?, overlays: [OverlayRegion], region: String = "UAE") async throws -> String {
        let result = try await functions.httpsCallable("generateEstimate").call([
            "projectId": projectId,
            "roomId": roomId ?? "",
            "overlayCount": overlays.count,
            "overlays": overlays.map { overlay in
                [
                    "materialPreset": overlay.materialPreset,
                    "widthMeters": overlay.widthMeters,
                    "heightMeters": overlay.heightMeters
                ]
            },
            "region": region
        ])
        let data = result.data as? [String: Any]
        let total = data?["total"] as? Double ?? 0
        let currency = data?["currency"] as? String ?? ""
        return "Estimate: \(String(format: "%.2f", total)) \(currency)"
    }
}
