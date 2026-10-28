import SwiftUI
import RealityKit
import ARKit

struct ARCameraPreview: UIViewRepresentable {
    @Binding var planeCount: Int
    @Binding var anchorCount: Int
    @Binding var estimatedWidth: Double
    @Binding var estimatedLength: Double

    func makeUIView(context: Context) -> ARView {
        let arView = ARView(frame: .zero)
        context.coordinator.attachSession(arView.session)

        let config = ARWorldTrackingConfiguration()
        config.planeDetection = [.horizontal, .vertical]
        arView.session.run(config, options: [.resetTracking, .removeExistingAnchors])
        return arView
    }

    func updateUIView(_ uiView: ARView, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(
            planeCount: $planeCount,
            anchorCount: $anchorCount,
            estimatedWidth: $estimatedWidth,
            estimatedLength: $estimatedLength
        )
    }

    final class Coordinator: NSObject, ARSessionDelegate {
        @Binding private var planeCount: Int
        @Binding private var anchorCount: Int
        @Binding private var estimatedWidth: Double
        @Binding private var estimatedLength: Double
        private var trackedPlaneIds = Set<UUID>()
        private var maxWidth: Float = 0
        private var maxLength: Float = 0

        init(
            planeCount: Binding<Int>,
            anchorCount: Binding<Int>,
            estimatedWidth: Binding<Double>,
            estimatedLength: Binding<Double>
        ) {
            _planeCount = planeCount
            _anchorCount = anchorCount
            _estimatedWidth = estimatedWidth
            _estimatedLength = estimatedLength
        }

        func attachSession(_ session: ARSession) {
            session.delegate = self
        }

        func session(_ session: ARSession, didAdd anchors: [ARAnchor]) {
            let addedPlaneIds = anchors.compactMap { ($0 as? ARPlaneAnchor)?.identifier }
            for id in addedPlaneIds { trackedPlaneIds.insert(id) }
            planeCount = trackedPlaneIds.count
            anchorCount += anchors.count
            updateExtents(from: anchors)
        }

        func session(_ session: ARSession, didUpdate anchors: [ARAnchor]) {
            updateExtents(from: anchors)
        }

        private func updateExtents(from anchors: [ARAnchor]) {
            let planes = anchors.compactMap { $0 as? ARPlaneAnchor }
            for plane in planes {
                maxWidth = max(maxWidth, plane.extent.x)
                maxLength = max(maxLength, plane.extent.z)
            }
            estimatedWidth = Double(maxWidth)
            estimatedLength = Double(maxLength)
        }
    }
}
