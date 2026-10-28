# Release Readiness and QA Matrix (UAE/US)

## Release Checklist

- Firebase environments validated:
  - `dev` and `prod` configs separated
  - Firestore rules and indexes deployed
- Cloud Functions deployed:
  - `createProject`
  - `generateEstimate`
  - `createShareLink`
  - `buildSharePackage`
- Crashlytics and analytics events verified.
- Share link expiry and ownership checks validated.
- Store submission metadata prepared (Android + iOS).

## Device QA Matrix

### Android

- Pixel 8 (Android 14)
- Samsung S23 (Android 14)
- Samsung A54 (Android 13)
- OnePlus 11 (Android 14)

### iOS

- iPhone 15 Pro (iOS 18)
- iPhone 14 (iOS 17)
- iPhone 13 (iOS 17)
- iPad (latest iPadOS supported)

## Test Scenarios

1. Auth + project create/load.
2. AR scan telemetry updates and room save.
3. Overlay add/edit/transform/material preview.
4. Undo/redo and batch material apply.
5. Save/load design versions and compare output.
6. Generate estimate and validate regional currency.
7. Create share link and verify expiry format.
8. Build share package and validate payload summary.

## Exit Criteria

- No blocker or critical defects open.
- AR scan works on all required test devices.
- Estimate and share workflows pass in both regions.
- App startup crash-free rate target met in staging.
