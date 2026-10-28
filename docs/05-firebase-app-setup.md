# Firebase App Setup (Android + iOS)

## 1) Create Firebase Projects

- `ar-home-design-dev`
- `ar-home-design-prod`

## 2) Register Mobile Apps

### Android

- Package name (example): `com.arhomedesign`
- Download `google-services.json`
- Place file at:
  - `android-app/app/google-services.json`

### iOS

- Bundle ID (example): `com.arhomedesign.ios`
- Download `GoogleService-Info.plist`
- Add to Xcode target:
  - `ios-app/ARRoomDesigner/GoogleService-Info.plist`

## 3) Enable Firebase Services

- Authentication:
  - Email/Password
- Firestore:
  - Native mode
- Storage:
  - Default bucket
- Crashlytics:
  - Both apps
- Analytics:
  - Both apps

## 4) Deploy Backend + Rules

From project root:

- `firebase use --add`
- `firebase deploy --only firestore:rules`
- `firebase deploy --only firestore:indexes`
- `cd backend/functions && npm install && npm run build`
- `firebase deploy --only functions`

## 5) Smoke Test Checklist

- Android login/register works.
- iOS login/register works.
- Create project works from both apps.
- Firestore shows owner-based project documents.
