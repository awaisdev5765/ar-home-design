# Run Locally (Android + iOS + Firebase)

## Prerequisites

- Android Studio (latest stable)
- Xcode 16+
- Node.js 20+
- Firebase CLI
- XcodeGen (`brew install xcodegen`)

## 1) Firebase Config Files

- Android: place `google-services.json` at `android-app/app/google-services.json`
- iOS: place `GoogleService-Info.plist` at `ios-app/ARRoomDesigner/GoogleService-Info.plist`

## 2) Backend Functions

From project root:

- `cd backend/functions`
- `npm install`
- `npm run build`
- `cd ../..`
- `firebase deploy --only functions,firestore:rules,firestore:indexes`

## 3) Android Run

From `android-app`:

- `echo "sdk.dir=/Users/airtechsolutions/Library/Android/sdk" > local.properties`
- `gradle wrapper --gradle-version 8.11.1`
- `./gradlew :app:assembleDebug`

Then open `android-app` in Android Studio and run on emulator/device.

## 4) iOS Run

From `ios-app`:

- `xcodegen generate`
- Open generated `ARRoomDesigner.xcodeproj`
- Select signing team and run on simulator/device

## 5) Smoke Test

1. Register a user in Android.
2. Login with same user in iOS.
3. Create project from Android.
4. Refresh projects in iOS and verify same project appears.
