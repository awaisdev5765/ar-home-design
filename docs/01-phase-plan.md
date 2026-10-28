# Phase-by-Phase Delivery Plan

## Phase 0 - Discovery and Setup (2 weeks)

Objective: lock scope, architecture, and environment.

- Finalize product requirements for MVP.
- Define technical architecture for Android, iOS, and Firebase.
- Setup Firebase project (dev and prod environments).
- Setup repos, branching strategy, and CI.
- Define design system and core navigation.

Exit criteria:

- Signed MVP scope and acceptance criteria.
- Working skeleton apps (Android and iOS) connected to Firebase Auth.
- Firestore schema v1 and security rules v1 drafted.

## Phase 1 - App Foundation (2 weeks)

Objective: release-ready app foundations in both mobile apps.

- Authentication flow.
- Project listing and project creation.
- Basic profile and settings.
- Error handling, logging, and analytics events baseline.

Exit criteria:

- Users can sign in, create a project, and load saved projects from Firestore.

## Phase 2 - AR Capture and Space Mapping (4 weeks)

Objective: capture room and detect surfaces.

- ARKit integration in iOS app.
- ARCore integration in Android app.
- Plane detection for walls/floors.
- Persist scan session metadata.

Exit criteria:

- User can complete a room scan and reopen the same scan session.

## Phase 3 - Design Interactions (4 weeks)

Objective: make design editing usable.

- Apply wall colors/materials.
- Apply flooring presets.
- Place/move/rotate/scale furniture assets.
- Save snapshots and revision history.

Exit criteria:

- User can create and save at least one complete design version.

## Phase 4 - Cost Estimation and Sharing (3 weeks)

Objective: introduce practical value for decision-making.

- Material catalog management.
- Basic estimation engine in Cloud Functions.
- Design share links and snapshot export.

Exit criteria:

- User can generate estimate summary and share design output.

## Phase 5 - QA, Performance, and Launch (3 weeks)

Objective: production launch readiness.

- Device compatibility testing matrix.
- Performance profiling and memory optimizations.
- Security hardening and store compliance.
- App Store and Play Store submissions.

Exit criteria:

- Production release candidate approved and deployed.

## Overall Timeline

- Fast MVP: 18 weeks
- Safer timeline with wider device QA: 20 to 24 weeks

## Team Recommendation

- 1 Android Engineer
- 1 iOS Engineer
- 1 Backend Engineer
- 1 AR/3D Engineer
- 1 QA Engineer
- 1 Product/Delivery Manager
