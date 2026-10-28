# Phase 5 - Launch Hardening

## Goal

Prepare the MVP for production launch with stability, observability, and release process controls.

## Deliverables

### 1) Performance Baseline and Profiling Plan

- Measure app startup time (cold/warm/hot).
- Measure AR scan session stability:
  - FPS target
  - memory footprint
  - thermal behavior on mid-range devices
- Measure network latency for:
  - estimate generation
  - share link generation
  - share package generation

Acceptance target:

- No severe frame drops in basic scan/edit flow on target devices.
- No repeated OOM/crash in 20-minute continuous use session.

### 2) Crash and Error Triage Flow

- Crashlytics enabled for Android and iOS.
- Error categories and ownership:
  - auth failures
  - firestore permission failures
  - functions invocation failures
  - AR session failures
- Define triage SLA:
  - blocker: immediate hotfix
  - critical: within 24h
  - major: within 72h

### 3) Release Branching and Freeze Protocol

- Create release branch per milestone.
- Code freeze window before submission.
- Final regression pass after freeze.
- Tag release candidate build numbers.

### 4) Store Submission Readiness

- App privacy disclosures prepared.
- Camera permission usage text verified.
- App metadata:
  - title
  - subtitle/short description
  - long description
  - screenshots (Android/iOS)
  - support and privacy URLs
- Version and changelog ready.

### 5) Production Monitoring Plan

- Daily checks for first 14 days:
  - crash-free users %
  - function error rate
  - estimate generation success %
  - share link creation success %
- Alert thresholds:
  - crash-free users below 98%
  - function failure rate above 3%

## Phase 5 Exit Criteria

- UAT script pass rate >= 95%.
- No open blocker defects.
- Store submissions accepted.
- Production monitoring dashboard and ownership agreed.
