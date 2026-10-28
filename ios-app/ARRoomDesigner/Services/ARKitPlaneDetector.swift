import Foundation
import ARKit

final class ARKitPlaneDetector: NSObject, ARSessionDelegate {
    private let session = ARSession()
    private(set) var currentPlaneCount: Int = 0

    override init() {
        super.init()
        session.delegate = self
    }

    func start() -> Bool {
        guard ARWorldTrackingConfiguration.isSupported else { return false }
        let configuration = ARWorldTrackingConfiguration()
        configuration.planeDetection = [.horizontal, .vertical]
        session.run(configuration, options: [.resetTracking, .removeExistingAnchors])
        return true
    }

    func stop() {
        session.pause()
        currentPlaneCount = 0
    }

    func session(_ session: ARSession, didAdd anchors: [ARAnchor]) {
        let added = anchors.compactMap { $0 as? ARPlaneAnchor }.count
        currentPlaneCount += added
    }
}
