import Foundation

struct DesignVersion: Identifiable {
    let id: String
    let projectId: String
    let roomId: String
    let ownerId: String
    let versionNumber: Int
    let notes: String
    let createdAt: TimeInterval
}
