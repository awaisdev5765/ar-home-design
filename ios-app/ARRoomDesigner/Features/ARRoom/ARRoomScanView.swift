import SwiftUI

struct ARRoomScanView: View {
    let project: Project
    @StateObject private var viewModel = ARRoomScanViewModel()
    @State private var roomName = "Living Room"
    @State private var widthMeters = "4.0"
    @State private var lengthMeters = "5.0"
    @State private var heightMeters = "2.8"
    @State private var previewPlanes = 0
    @State private var previewAnchors = 0
    @State private var previewWidth = 0.0
    @State private var previewLength = 0.0
    @State private var versionNotes = "Phase 3 version"

    var body: some View {
        VStack(spacing: 12) {
            Text("AR Scan - \(project.title)")
                .font(.headline)
            Text("ARKit session enabled. Detect planes then save room scan.")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            TextField("Room Name", text: $roomName)
                .textFieldStyle(.roundedBorder)
            TextField("Width (m)", text: $widthMeters)
                .textFieldStyle(.roundedBorder)
            TextField("Length (m)", text: $lengthMeters)
                .textFieldStyle(.roundedBorder)
            TextField("Height (m)", text: $heightMeters)
                .textFieldStyle(.roundedBorder)
            Button(viewModel.arSessionActive ? "AR Session Active" : "Start AR Session") {
                viewModel.startSession()
            }
            .buttonStyle(.bordered)

            if viewModel.arSessionActive {
                ARCameraPreview(
                    planeCount: $previewPlanes,
                    anchorCount: $previewAnchors,
                    estimatedWidth: $previewWidth,
                    estimatedLength: $previewLength
                )
                    .frame(height: 220)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            Text("Detected Planes: \(viewModel.detectedPlanes)")
            Text("Detected Anchors: \(viewModel.detectedAnchors)")
            Text("Estimated Size: \(String(format: "%.2f", viewModel.estimatedWidthMeters))m x \(String(format: "%.2f", viewModel.estimatedLengthMeters))m x \(String(format: "%.2f", viewModel.estimatedHeightMeters))m")
            Text("Estimated Confidence: \(String(format: "%.2f", viewModel.estimatedConfidence))")
            Text("Selected Surface: \(viewModel.selectedSurfaceType)")
            Text("Active Room: \(viewModel.activeRoomId ?? "none")")
            Text("Material: \(viewModel.selectedMaterialPreset) (\(viewModel.selectedColorHex))")
            if !viewModel.materials.isEmpty {
                Text("Material Catalog")
                ForEach(viewModel.materials) { item in
                    Button("\(item.name) (\(item.category))") {
                        viewModel.selectMaterialPreset(item.presetCode)
                        viewModel.setColorHex(item.colorHex)
                    }
                    .buttonStyle(.bordered)
                }
            }
            Button("Material Preset: Default Paint") { viewModel.selectMaterialPreset("default_paint") }
                .buttonStyle(.bordered)
            Button("Material Preset: Oak Wood") { viewModel.selectMaterialPreset("oak_wood") }
                .buttonStyle(.bordered)
            Button("Material Preset: Marble Tile") { viewModel.selectMaterialPreset("marble_tile") }
                .buttonStyle(.bordered)
            TextField("Material Color Hex", text: Binding(
                get: { viewModel.selectedColorHex },
                set: { viewModel.setColorHex($0) }
            ))
            .textFieldStyle(.roundedBorder)
            Button("Select Surface: Wall") { viewModel.selectSurfaceType("wall") }
                .buttonStyle(.bordered)
            Button("Select Surface: Floor") { viewModel.selectSurfaceType("floor") }
                .buttonStyle(.bordered)
            Button("Select Surface: Ceiling") { viewModel.selectSurfaceType("ceiling") }
                .buttonStyle(.bordered)

            Button("Capture and Save Scan") {
                Task {
                    await viewModel.saveScan(
                        projectId: project.id,
                        roomName: roomName,
                        widthMeters: Double(widthMeters) ?? 0.0,
                        lengthMeters: Double(lengthMeters) ?? 0.0,
                        heightMeters: Double(heightMeters) ?? 0.0
                    )
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(!viewModel.arSessionActive)

            Button("Add Design Overlay Region") {
                Task { await viewModel.addOverlayPrimitive(projectId: project.id) }
            }
            .buttonStyle(.bordered)
            TextField("Version Notes", text: $versionNotes)
                .textFieldStyle(.roundedBorder)
            Button("Save Design Version") {
                Task { await viewModel.saveDesignVersion(projectId: project.id, notes: versionNotes) }
            }
            .buttonStyle(.bordered)
            Button("Generate Estimate") {
                Task { await viewModel.generateEstimate(projectId: project.id) }
            }
            .buttonStyle(.bordered)
            if !viewModel.estimateSummary.isEmpty {
                Text(viewModel.estimateSummary)
            }
            Button("Compare Latest Versions") {
                viewModel.compareLatestVersions()
            }
            .buttonStyle(.bordered)
            if !viewModel.compareSummary.isEmpty {
                Text(viewModel.compareSummary)
            }
            Button("Build Export Summary") {
                viewModel.buildExportSummary(projectId: project.id)
            }
            .buttonStyle(.bordered)
            if !viewModel.exportSummary.isEmpty {
                Text(viewModel.exportSummary)
            }
            Button("Create Share Link") {
                Task { await viewModel.createShareLink(projectId: project.id) }
            }
            .buttonStyle(.bordered)
            if !viewModel.shareUrl.isEmpty {
                Text(viewModel.shareUrl)
            }
            Button("Build Share Package") {
                Task { await viewModel.buildSharePackage(projectId: project.id) }
            }
            .buttonStyle(.bordered)
            if !viewModel.sharePackageSummary.isEmpty {
                Text(viewModel.sharePackageSummary)
            }
            Button("Apply Material To Previewed Overlays") {
                Task { await viewModel.applyMaterialToPreviewedOverlays(projectId: project.id) }
            }
            .buttonStyle(.bordered)
            Button("Clear Preview Selection") {
                viewModel.clearMaterialPreviewSelection()
            }
            .buttonStyle(.bordered)
            Button("Undo Overlay Edit") {
                Task { await viewModel.undoOverlayEdits(projectId: project.id) }
            }
            .buttonStyle(.bordered)
            .disabled(!viewModel.canUndoOverlayEdit)
            Button("Redo Overlay Edit") {
                Task { await viewModel.redoOverlayEdits(projectId: project.id) }
            }
            .buttonStyle(.bordered)
            .disabled(!viewModel.canRedoOverlayEdit)

            Button("Stop AR Session") {
                viewModel.stopSession()
            }
            .buttonStyle(.bordered)
            .disabled(!viewModel.arSessionActive)

            Button("Refresh Scans") {
                Task { await viewModel.loadScans(projectId: project.id) }
            }
            .buttonStyle(.bordered)

            List(viewModel.scans) { scan in
                VStack(alignment: .leading, spacing: 4) {
                    Text(scan.name).font(.headline)
                    Button("Use This Room for Overlays") {
                        Task { await viewModel.selectActiveRoom(scan.id, projectId: project.id) }
                    }
                    .buttonStyle(.bordered)
                    Text("Planes: \(scan.planeCount)")
                    Text("Anchors: \(scan.anchorCount)")
                    Text("Size: \(scan.widthMeters)m x \(scan.lengthMeters)m x \(scan.heightMeters)m")
                    Text("Confidence: \(scan.confidence)")
                    Text("Status: \(scan.status)")
                        .foregroundStyle(.secondary)
                }
            }

            ForEach(viewModel.overlays) { overlay in
                VStack(alignment: .leading, spacing: 2) {
                    Text(overlay.name)
                    Text("Surface: \(overlay.surfaceType)")
                        .foregroundStyle(.secondary)
                    Text("Material: \(overlay.materialPreset) (\(overlay.colorHex))")
                    Text("Pos: (\(String(format: "%.2f", overlay.positionX)), \(String(format: "%.2f", overlay.positionY)), \(String(format: "%.2f", overlay.positionZ)))")
                    Text("Size: \(String(format: "%.2f", overlay.widthMeters))m x \(String(format: "%.2f", overlay.heightMeters))m, Rot: \(String(format: "%.1f", overlay.rotationDeg))deg")
                    Text(viewModel.previewAppliedOverlayIds.contains(overlay.id) ? "Preview: Applied" : "Preview: Not Applied")
                    Button("Toggle Material Preview") {
                        viewModel.toggleMaterialPreview(overlay.id)
                    }
                    .buttonStyle(.bordered)
                    Button("Save Current Material to Overlay") {
                        Task { await viewModel.persistOverlayMaterial(projectId: project.id, overlayId: overlay.id) }
                    }
                    .buttonStyle(.bordered)
                    Button("Move +X") {
                        Task { await viewModel.updateOverlayTransform(projectId: project.id, overlayId: overlay.id, deltaX: 0.1) }
                    }
                    .buttonStyle(.bordered)
                    Button("Increase Size") {
                        Task { await viewModel.updateOverlayTransform(projectId: project.id, overlayId: overlay.id, scaleDelta: 0.1) }
                    }
                    .buttonStyle(.bordered)
                    Button("Rotate +15deg") {
                        Task { await viewModel.updateOverlayTransform(projectId: project.id, overlayId: overlay.id, rotationDelta: 15) }
                    }
                    .buttonStyle(.bordered)
                }
                .padding(8)
                .background(viewModel.previewAppliedOverlayIds.contains(overlay.id) ? colorFromHex(overlay.colorHex).opacity(0.25) : Color.clear)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            ForEach(viewModel.versions) { version in
                VStack(alignment: .leading, spacing: 2) {
                    Text("Version v\(version.versionNumber)")
                    Text("Notes: \(version.notes)")
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding()
        .task {
            await viewModel.loadScans(projectId: project.id)
        }
        .onChange(of: previewPlanes) { _, _ in
            viewModel.updateTelemetry(planes: previewPlanes, anchors: previewAnchors)
        }
        .onChange(of: previewAnchors) { _, _ in
            viewModel.updateTelemetry(planes: previewPlanes, anchors: previewAnchors)
        }
        .onChange(of: previewWidth) { _, newValue in
            viewModel.updateEstimatedBounds(width: newValue, length: previewLength)
            if newValue > 0 { widthMeters = String(format: "%.2f", newValue) }
        }
        .onChange(of: previewLength) { _, newValue in
            viewModel.updateEstimatedBounds(width: previewWidth, length: newValue)
            if newValue > 0 { lengthMeters = String(format: "%.2f", newValue) }
        }
    }
}

private func colorFromHex(_ hex: String) -> Color {
    var cleaned = hex.trimmingCharacters(in: .whitespacesAndNewlines)
    if cleaned.hasPrefix("#") { cleaned.removeFirst() }
    guard cleaned.count == 6, let value = UInt(cleaned, radix: 16) else { return .white }
    let r = Double((value >> 16) & 0xFF) / 255.0
    let g = Double((value >> 8) & 0xFF) / 255.0
    let b = Double(value & 0xFF) / 255.0
    return Color(red: r, green: g, blue: b)
}
