# Phase 4 Kickoff - Estimation Refinement and Sharing

Phase 4 has started with backend and mobile support for production-facing estimate outputs and secure share links.

## Implemented in this step

- Backend callable `createShareLink`:
  - creates `shareLinks` entry
  - returns expiring share URL
- Estimate refinement in backend:
  - `generateEstimate` now calculates line items using:
    - overlay material preset
    - overlay area approximation (`widthMeters * heightMeters`)
    - region pricing from `materialCatalog`
- Mobile integration:
  - Android + iOS can request share link and display URL
  - Android + iOS estimate hooks remain connected to refined backend output

## Next in Phase 4

All items are now implemented:

1. Export share package payload via `buildSharePackage`.
2. Release-readiness checklist and QA matrix added.
3. Share-link hardening added:
   - project ownership validation
   - per-project basic rate limit
   - bounded expiry window

## Phase 4 status

Phase 4 is complete.

## Handover to Phase 5

- Launch hardening plan: `docs/11-phase-5-launch-hardening.md`
- UAT execution script: `docs/12-uat-script.md`
