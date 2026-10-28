# Sprint 01 Backlog (10 Working Days)

Sprint goal: establish project foundation and ship authentication + project creation on both platforms.

## Workstreams

### Product and Design

1. Finalize MVP user flows:
   - Sign in
   - Project list
   - Create project
2. Finalize low-fidelity wireframes for same flows.
3. Define acceptance criteria for each story.

Definition of done:

- Wireframes approved.
- Acceptance criteria published for all sprint tickets.

### Backend (Firebase)

1. Create Firebase `dev` project.
2. Configure Auth providers (email/password first).
3. Create Firestore collections from schema v1.
4. Implement rules v1.
5. Add Cloud Functions boilerplate.

Definition of done:

- Auth works in test app.
- Firestore read/write tested for valid owner.
- Unauthorized reads blocked.

### Android (Kotlin + Compose)

1. Setup app architecture skeleton (MVVM/Clean).
2. Integrate Firebase Auth and Firestore SDK.
3. Build auth screens (login/register).
4. Build project list screen.
5. Build create project form and save flow.

Definition of done:

- Android app signs in.
- Project create/load works from Firestore.
- Basic error states are handled.

### iOS (SwiftUI)

1. Setup app architecture skeleton (MVVM + Services).
2. Integrate Firebase Auth and Firestore SDK.
3. Build auth screens (login/register).
4. Build project list screen.
5. Build create project form and save flow.

Definition of done:

- iOS app signs in.
- Project create/load works from Firestore.
- Basic error states are handled.

### QA

1. Create test checklist for auth and project management.
2. Validate positive and negative auth flows.
3. Validate project CRUD smoke tests.
4. Report defects with severity and repro steps.

Definition of done:

- Test checklist completed.
- All blocker defects resolved or formally deferred.

## Suggested Daily Plan

- Day 1-2: Setup and architecture baseline.
- Day 3-4: Auth implementation both apps.
- Day 5-6: Project list and create project flow.
- Day 7-8: Integration and bug fixing.
- Day 9: QA regression pass.
- Day 10: Sprint demo and retrospective.

## Sprint Exit Checklist

- Both apps connect to same Firebase dev project.
- User can sign in and create a project from both apps.
- Firestore rules prevent cross-user data access.
- Crashlytics integrated in both apps.
- Demo script prepared for next sprint planning.
