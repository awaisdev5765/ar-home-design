# Phase 3 Kickoff - Design Versions and Material Library

Phase 3 has started with cross-platform implementation in Android and iOS.

## Delivered in this step

- Material catalog integration (`materialCatalog`) with fallback seed items.
- Design version save/load support (`designVersions`) linked to `projectId` + `roomId`.
- Estimate integration hook (`generateEstimate` callable) with mobile triggers.
- UI controls for:
  - selecting catalog materials
  - saving design versions with notes
  - listing saved versions in scan workflow
  - generating estimate summary from active overlay count

## Android additions

- Models:
  - `DesignVersion`
  - `MaterialCatalogItem`
- Repositories:
  - `DesignVersionRepository`
  - `MaterialCatalogRepository`
- ViewModel:
  - load materials and versions
  - save version snapshots
- Screen:
  - material catalog buttons
  - version notes input
  - save/list versions

## iOS additions

- Models:
  - `DesignVersion`
  - `MaterialCatalogItem`
- Services:
  - `DesignVersionService`
  - `MaterialCatalogService`
- ViewModel:
  - load materials and versions
  - save version snapshots
- View:
  - material catalog buttons
  - version notes input
  - save/list versions

## Phase 3 completion

- Added version compare mode (`vN` vs `vN-1`) in both apps.
- Added export/share summary builder (project, room, version, overlays, estimate).
- Upgraded estimate path to catalog-driven pricing:
  - backend `generateEstimate` now reads `materialCatalog` prices by `presetCode`
  - estimate payload now uses overlay material + area approximation (`width * height`)
  - supports region-specific pricing (`UAE`/`US`)

## Phase 3 status

Phase 3 is complete.
