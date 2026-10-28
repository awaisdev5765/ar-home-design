import Foundation

@MainActor
final class ARRoomScanViewModel: ObservableObject {
    @Published var scans: [RoomScan] = []
    @Published var isLoading = false
    @Published var arSessionActive = false
    @Published var detectedPlanes = 0
    @Published var detectedAnchors = 0
    @Published var estimatedWidthMeters: Double = 0
    @Published var estimatedLengthMeters: Double = 0
    @Published var estimatedHeightMeters: Double = 2.8
    @Published var estimatedConfidence: Double = 0
    @Published var selectedSurfaceType: String = "wall"
    @Published var selectedMaterialPreset: String = "default_paint"
    @Published var selectedColorHex: String = "#FFFFFF"
    @Published var activeRoomId: String?
    @Published var previewAppliedOverlayIds: Set<String> = []
    @Published var overlays: [OverlayRegion] = []
    @Published var versions: [DesignVersion] = []
    @Published var materials: [MaterialCatalogItem] = []
    @Published var estimateSummary: String = ""
    @Published var compareSummary: String = ""
    @Published var exportSummary: String = ""
    @Published var shareUrl: String = ""
    @Published var sharePackageSummary: String = ""
    @Published var canUndoOverlayEdit = false
    @Published var canRedoOverlayEdit = false
    @Published var errorMessage: String?

    private let service: RoomScanService
    private let overlayService: OverlayRegionService
    private let versionService: DesignVersionService
    private let materialService: MaterialCatalogService
    private let estimateService: EstimateService
    private let shareService: ShareService
    private var overlayUndoStack: [[OverlayRegion]] = []
    private var overlayRedoStack: [[OverlayRegion]] = []

    init(
        service: RoomScanService = RoomScanService(),
        overlayService: OverlayRegionService = OverlayRegionService(),
        versionService: DesignVersionService = DesignVersionService(),
        materialService: MaterialCatalogService = MaterialCatalogService(),
        estimateService: EstimateService = EstimateService(),
        shareService: ShareService = ShareService()
    ) {
        self.service = service
        self.overlayService = overlayService
        self.versionService = versionService
        self.materialService = materialService
        self.estimateService = estimateService
        self.shareService = shareService
    }

    func loadScans(projectId: String) async {
        isLoading = true
        errorMessage = nil
        do {
            scans = try await service.loadScans(projectId: projectId)
            if activeRoomId == nil { activeRoomId = scans.first?.id }
            overlays = try await overlayService.loadOverlays(projectId: projectId, roomId: activeRoomId)
            versions = try await versionService.loadVersions(projectId: projectId, roomId: activeRoomId)
            materials = try await materialService.loadMaterials()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func saveScan(
        projectId: String,
        roomName: String,
        widthMeters: Double,
        lengthMeters: Double,
        heightMeters: Double
    ) async {
        isLoading = true
        errorMessage = nil
        do {
            let metrics = RoomScanService.ScanCaptureMetrics(
                planeCount: detectedPlanes,
                anchorCount: detectedAnchors,
                widthMeters: widthMeters > 0 ? widthMeters : estimatedWidthMeters,
                lengthMeters: lengthMeters > 0 ? lengthMeters : estimatedLengthMeters,
                heightMeters: heightMeters > 0 ? heightMeters : estimatedHeightMeters,
                confidence: estimatedConfidence
            )
            let roomId = try await service.saveScan(projectId: projectId, roomName: roomName, metrics: metrics)
            activeRoomId = roomId
            scans = try await service.loadScans(projectId: projectId)
            overlays = try await overlayService.loadOverlays(projectId: projectId, roomId: activeRoomId)
            versions = try await versionService.loadVersions(projectId: projectId, roomId: activeRoomId)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    func startSession() {
        arSessionActive = true
    }

    func updateTelemetry(planes: Int, anchors: Int) {
        detectedPlanes = planes
        detectedAnchors = anchors
        estimatedConfidence = planes >= 6 ? 0.95 : (planes >= 3 ? 0.8 : (planes > 0 ? 0.6 : 0.0))
    }

    func updateEstimatedBounds(width: Double, length: Double) {
        if width > 0 { estimatedWidthMeters = width }
        if length > 0 { estimatedLengthMeters = length }
    }

    func selectSurfaceType(_ surfaceType: String) {
        selectedSurfaceType = surfaceType
    }

    func selectMaterialPreset(_ materialPreset: String) {
        selectedMaterialPreset = materialPreset
    }

    func setColorHex(_ colorHex: String) {
        selectedColorHex = colorHex
    }

    func selectActiveRoom(_ roomId: String, projectId: String) async {
        activeRoomId = roomId
        do {
            overlays = try await overlayService.loadOverlays(projectId: projectId, roomId: roomId)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func addOverlayPrimitive(projectId: String) async {
        do {
            guard let roomId = activeRoomId else {
                errorMessage = "Create or select a room scan first."
                return
            }
            let name = "Overlay Region \(overlays.count + 1)"
            _ = try await overlayService.addOverlay(
                projectId: projectId,
                roomId: roomId,
                name: name,
                surfaceType: selectedSurfaceType,
                materialPreset: selectedMaterialPreset,
                colorHex: selectedColorHex
            )
            overlays = try await overlayService.loadOverlays(projectId: projectId, roomId: roomId)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func toggleMaterialPreview(_ overlayId: String) {
        if previewAppliedOverlayIds.contains(overlayId) {
            previewAppliedOverlayIds.remove(overlayId)
        } else {
            previewAppliedOverlayIds.insert(overlayId)
        }
    }

    func clearMaterialPreviewSelection() {
        previewAppliedOverlayIds.removeAll()
    }

    func applyMaterialToPreviewedOverlays(projectId: String) async {
        if previewAppliedOverlayIds.isEmpty { return }
        snapshotOverlayStateForUndo()
        do {
            for overlayId in previewAppliedOverlayIds {
                try await overlayService.updateOverlayMaterial(
                    overlayId: overlayId,
                    materialPreset: selectedMaterialPreset,
                    colorHex: selectedColorHex
                )
            }
            overlays = try await overlayService.loadOverlays(projectId: projectId, roomId: activeRoomId)
            updateHistoryFlags()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func saveDesignVersion(projectId: String, notes: String) async {
        guard let roomId = activeRoomId else {
            errorMessage = "Select a room before saving version."
            return
        }
        do {
            _ = try await versionService.saveVersion(
                projectId: projectId,
                roomId: roomId,
                notes: notes,
                overlays: overlays
            )
            versions = try await versionService.loadVersions(projectId: projectId, roomId: roomId)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func generateEstimate(projectId: String) async {
        do {
            estimateSummary = try await estimateService.generateEstimate(
                projectId: projectId,
                roomId: activeRoomId,
                overlays: overlays
            )
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func compareLatestVersions() {
        guard versions.count >= 2 else {
            compareSummary = "Need at least two versions to compare."
            return
        }
        let latest = versions[0]
        let previous = versions[1]
        compareSummary = "Compare v\(latest.versionNumber) vs v\(previous.versionNumber): notes changed from '\(previous.notes)' to '\(latest.notes)'."
    }

    func buildExportSummary(projectId: String) {
        let latest = versions.first
        var lines: [String] = []
        lines.append("Project: \(projectId)")
        lines.append("Room: \(activeRoomId ?? "N/A")")
        if let latest {
            lines.append("Version: v\(latest.versionNumber)")
            lines.append("Version Notes: \(latest.notes)")
        }
        lines.append("Overlays: \(overlays.count)")
        lines.append("Estimate: \(estimateSummary.isEmpty ? "Not generated" : estimateSummary)")
        exportSummary = lines.joined(separator: "\n")
    }

    func createShareLink(projectId: String) async {
        do {
            let versionId = versions.first?.id
            shareUrl = try await shareService.createShareLink(projectId: projectId, versionId: versionId)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func buildSharePackage(projectId: String) async {
        do {
            sharePackageSummary = try await shareService.buildSharePackage(projectId: projectId, roomId: activeRoomId)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func persistOverlayMaterial(projectId: String, overlayId: String) async {
        do {
            snapshotOverlayStateForUndo()
            try await overlayService.updateOverlayMaterial(
                overlayId: overlayId,
                materialPreset: selectedMaterialPreset,
                colorHex: selectedColorHex
            )
            overlays = try await overlayService.loadOverlays(projectId: projectId, roomId: activeRoomId)
            updateHistoryFlags()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func updateOverlayTransform(
        projectId: String,
        overlayId: String,
        deltaX: Double = 0,
        deltaY: Double = 0,
        deltaZ: Double = 0,
        scaleDelta: Double = 0,
        rotationDelta: Double = 0
    ) async {
        guard let overlay = overlays.first(where: { $0.id == overlayId }) else { return }
        let nextWidth = max(0.1, overlay.widthMeters + scaleDelta)
        let nextHeight = max(0.1, overlay.heightMeters + scaleDelta)
        do {
            snapshotOverlayStateForUndo()
            try await overlayService.updateOverlayTransform(
                overlayId: overlayId,
                positionX: overlay.positionX + deltaX,
                positionY: overlay.positionY + deltaY,
                positionZ: overlay.positionZ + deltaZ,
                widthMeters: nextWidth,
                heightMeters: nextHeight,
                rotationDeg: overlay.rotationDeg + rotationDelta
            )
            overlays = try await overlayService.loadOverlays(projectId: projectId, roomId: activeRoomId)
            updateHistoryFlags()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func undoOverlayEdits(projectId: String) async {
        guard let previous = overlayUndoStack.popLast() else { return }
        overlayRedoStack.append(overlays)
        await applyOverlayState(projectId: projectId, snapshot: previous)
        updateHistoryFlags()
    }

    func redoOverlayEdits(projectId: String) async {
        guard let next = overlayRedoStack.popLast() else { return }
        overlayUndoStack.append(overlays)
        await applyOverlayState(projectId: projectId, snapshot: next)
        updateHistoryFlags()
    }

    private func snapshotOverlayStateForUndo() {
        overlayUndoStack.append(overlays)
        overlayRedoStack.removeAll()
        updateHistoryFlags()
    }

    private func updateHistoryFlags() {
        canUndoOverlayEdit = !overlayUndoStack.isEmpty
        canRedoOverlayEdit = !overlayRedoStack.isEmpty
    }

    private func applyOverlayState(projectId: String, snapshot: [OverlayRegion]) async {
        do {
            for overlay in snapshot {
                try await overlayService.updateOverlayMaterial(
                    overlayId: overlay.id,
                    materialPreset: overlay.materialPreset,
                    colorHex: overlay.colorHex
                )
                try await overlayService.updateOverlayTransform(
                    overlayId: overlay.id,
                    positionX: overlay.positionX,
                    positionY: overlay.positionY,
                    positionZ: overlay.positionZ,
                    widthMeters: overlay.widthMeters,
                    heightMeters: overlay.heightMeters,
                    rotationDeg: overlay.rotationDeg
                )
            }
            overlays = try await overlayService.loadOverlays(projectId: projectId, roomId: activeRoomId)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func stopSession() {
        arSessionActive = false
        detectedPlanes = 0
        detectedAnchors = 0
    }
}
