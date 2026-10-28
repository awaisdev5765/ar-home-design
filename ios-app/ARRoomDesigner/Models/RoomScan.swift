import Foundation

struct RoomScan: Identifiable {
    let id: String
    let projectId: String
    let ownerId: String
    let name: String
    let planeCount: Int
    let anchorCount: Int
    let widthMeters: Double
    let lengthMeters: Double
    let heightMeters: Double
    let confidence: Double
    let status: String
}
