# Phase 2 Kickoff - AR Capture and Room Mapping

Phase 2 has started with AR scan data flow stubs across Android and iOS.

## Implemented in this kickoff

- Added shared `RoomScan` model in both apps.
- Added Firebase `rooms` write/read services:
  - Save scan payload
  - Load scans per project
- Added AR scan screens:
  - Android: `ARScanScreen` + `ARScanViewModel`
  - iOS: `ARRoomScanView` + `ARRoomScanViewModel`
- Connected project list to AR room scan screen by project ID.
- Added real AR session foundations:
  - Android `ARCorePlaneDetector` using ARCore `Session` with horizontal/vertical plane detection
  - iOS `ARKitPlaneDetector` using `ARSessionDelegate` and `ARPlaneAnchor`
- Added Phase 2.1 telemetry and richer metadata:
  - Live plane polling on Android while AR session is active
  - iOS `ARCameraPreview` (`RealityKit ARView`) embedded in scan screen
  - Scan metadata now stores `anchorCount`, room dimensions, and `confidence`

## Phase 2.2 progress

- Android:
  - Added visible camera preview (`CameraX`) on scan screen
  - Added ARCore room estimate extraction (`widthMeters`, `lengthMeters`, `confidence`)
  - Auto-populates room dimensions from tracked planes
  - Added initial design overlay primitives list
- iOS:
  - Extended AR preview coordinator to estimate room bounds from `ARPlaneAnchor.extent`
  - Auto-populates width/length and confidence in scan viewmodel
  - Added initial design overlay primitives list

## Phase 2.3 progress

- Overlay regions are now persisted in Firestore collection `overlayRegions`.
- Added selectable surface types for overlays:
  - `wall`
  - `floor`
  - `ceiling`
- Android and iOS both support:
  - Select surface type
  - Add overlay region
  - Reload overlays from backend
- Firestore rules and indexes were extended for `overlayRegions`.

## Phase 2.4 progress

- Overlay regions are now linked to `roomId` (not only `projectId`).
- Added editable overlay material properties:
  - `materialPreset`
  - `colorHex`
- Added basic material apply preview in app state:
  - toggle preview applied/not applied per overlay
- Android and iOS both now support:
  - selecting active room scan for overlay operations
  - assigning material preset and color
  - persisting overlay material updates to Firestore

## Phase 2.5 progress

- Added per-overlay transform and bounds data:
  - `positionX`, `positionY`, `positionZ`
  - `widthMeters`, `heightMeters`
  - `rotationDeg`
- Android and iOS both now support transform editing actions:
  - move overlay
  - resize overlay
  - rotate overlay
- Material preview is now visually rendered in overlay cards as tint (by `colorHex`) when preview is toggled.

## Phase 2.6 progress

- Added undo/redo for overlay edits (material + transform) on both platforms.
- Undo/redo operates on persisted overlay state snapshots and reloads overlays after apply.
- Added UI controls:
  - `Undo Overlay Edit`
  - `Redo Overlay Edit`

## Phase 2 completion

- Added batch material apply workflow:
  - select preview overlays
  - apply selected material preset/color to all selected overlays
  - clear preview selection quickly
- Phase 2 delivered outcomes:
  - AR scan capture baseline (ARCore/ARKit session telemetry)
  - room metadata persistence (`rooms`)
  - overlay region lifecycle (`overlayRegions`)
  - surface typing (`wall`/`floor`/`ceiling`)
  - material assignment + visual preview tint
  - transform editing (position/size/rotation)
  - undo/redo + batch apply tools for safe iteration

## Current behavior

- User opens a project.
- User starts AR session and gets detected plane count from ARCore/ARKit.
- User can save scan metadata (`roomName`, detected `planeCount`) into `rooms`.
- User can reload existing scan records from Firestore.

## Next technical tasks

1. Replace mock scan action with real AR session integration:
   - Android ARCore plane extraction
   - iOS ARKit plane extraction
2. Persist richer scan metadata:
   - dimensions
   - anchor references
   - surface confidence
3. Add project detail screen with tabs:
   - Scan
   - Design
   - Estimate
