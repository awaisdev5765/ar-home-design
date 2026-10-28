import Foundation

struct OverlayRegion: Identifiable {
    let id: String
    let projectId: String
    let roomId: String
    let ownerId: String
    let name: String
    let surfaceType: String
    let materialPreset: String
    let colorHex: String
    let positionX: Double
    let positionY: Double
    let positionZ: Double
    let widthMeters: Double
    let heightMeters: Double
    let rotationDeg: Double
    let status: String
}
