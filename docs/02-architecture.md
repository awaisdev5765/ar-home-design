# Technical Architecture

## Stack

- Android: Kotlin, Jetpack Compose, ARCore, Hilt, Coroutines, Retrofit
- iOS: Swift, SwiftUI, ARKit, Combine/async-await
- Backend: Firebase Auth, Firestore, Storage, Cloud Functions, Analytics, Crashlytics

## High-Level Components

1. Mobile Clients
   - Authentication
   - Project and room management
   - AR rendering and user interactions
   - Cost estimate display and share flow

2. Firebase Backend
   - Auth for identity
   - Firestore for structured project data
   - Storage for scan artifacts and snapshots
   - Cloud Functions for estimate calculations and share token generation

3. Optional Asset Service (Phase 3+)
   - CDN-backed static furniture/material assets
   - Admin upload tool for new assets

## Mobile Module Boundaries

### Android Modules

- `app`: navigation and DI setup
- `feature-auth`
- `feature-projects`
- `feature-ar-room`
- `feature-estimation`
- `core-network`
- `core-data`
- `core-ui`

### iOS Modules (Package Targets or grouped folders)

- `AppCore`
- `FeatureAuth`
- `FeatureProjects`
- `FeatureARRoom`
- `FeatureEstimation`
- `SharedUI`
- `DataLayer`

## Data Flow

1. User signs in via Firebase Auth.
2. User creates project document in Firestore.
3. AR session starts and captures room metadata.
4. Session and design version are saved in Firestore.
5. Snapshots or heavy artifacts uploaded to Storage.
6. Estimate request sent to Cloud Function.
7. Function returns estimate summary saved to Firestore.

## Environment Strategy

- `dev`: active development and QA testing.
- `prod`: production release only.

Each environment has:

- Separate Firebase project
- Separate bundle IDs/application IDs
- Separate Storage bucket path prefix

## Security and Compliance

- Firestore rules scoped by authenticated user ownership.
- Storage path access restricted to owner.
- Sensitive operations (estimate generation, share token create) only through Cloud Functions.
- Audit fields on all write operations:
  - `createdAt`
  - `createdBy`
  - `updatedAt`
  - `updatedBy`

## Observability

- Crashlytics for runtime errors.
- Performance Monitoring for startup/network traces.
- Analytics events:
  - `login_success`
  - `project_created`
  - `scan_completed`
  - `design_saved`
  - `estimate_generated`
  - `design_shared`
