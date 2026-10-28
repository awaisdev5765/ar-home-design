# AR Home Design

AR Home Design is a cross-platform interior-design workspace for planning rooms, applying materials, placing furniture in augmented reality, saving design snapshots, and sharing a project with collaborators.

The repository contains Android, iOS, and Firebase foundations for the product.

## Product flow

```text
Create project → Scan room → Apply materials → Place furniture in AR
      → Save design version → Estimate cost → Share for feedback
```

## Current capabilities

- Android app: Kotlin + Jetpack Compose
- iOS app: SwiftUI
- Backend: Firebase (Auth, Firestore, Storage, Functions)
- Room scan session storage, material application, furniture placement, design snapshots, sharing, and cost-estimation foundations

## Repository layout

```text
ar-home-design/
├── android-app/        # Android app and emulator scripts
├── ios-app/            # SwiftUI/XcodeGen iOS app
├── backend/functions/  # Firebase Cloud Functions
├── docs/               # Architecture, phases, schema, QA, UAT, and setup guides
├── firestore.rules
├── firestore.indexes.json
└── firebase.json
```

## Requirements

- Android Studio with an Android SDK and emulator
- Xcode 15+ and iOS 17+ for the iOS app
- Node.js 18+ and npm for Firebase Functions
- Firebase CLI and a configured Firebase project

## Run locally

### Android

1. Open `android-app` in Android Studio.
2. Create/select an ARCore emulator or connect a compatible device.
3. Add your local `android-app/app/google-services.json` and set `android-app/local.properties`.

```bash
cd android-app
./gradlew assembleDebug
./run-on-emulator.sh
```

### iOS

1. Open the iOS project in Xcode, or run `xcodegen generate` from `ios-app`.
2. Add your local `GoogleService-Info.plist` to the iOS target.
3. Use a simulator for navigation and a LiDAR-capable physical device for RoomPlan/AR.

```bash
cd ios-app
xcodegen generate
xcodebuild -project ARRoomDesigner.xcodeproj -scheme ARRoomDesigner \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' build
```

### Firebase backend

```bash
cd backend/functions
npm install
npm run build
firebase deploy --only functions,firestore:rules,firestore:indexes
```

Read `docs/05-firebase-app-setup.md` before connecting a Firebase project. Never commit downloaded Firebase configuration or service-account credentials.

## Documents

- `docs/01-phase-plan.md`: End-to-end delivery phases
- `docs/02-architecture.md`: System architecture and app module boundaries
- `docs/03-firebase-schema.md`: Firestore collections, documents, indexes, and rules
- `docs/04-sprint-01-backlog.md`: First sprint backlog with task ownership and definition of done
- `docs/05-firebase-app-setup.md`: Firebase mobile app setup and deployment steps
- `docs/06-run-locally.md`: Local run instructions for Android, iOS, and Firebase
- `docs/07-phase-2-kickoff.md`: AR scan data-flow implementation status and next tasks
- `docs/08-phase-3-kickoff.md`: Phase 3 material catalog and design version progress
- `docs/09-phase-4-kickoff.md`: Phase 4 estimation refinement and sharing progress
- `docs/10-release-readiness-qa-matrix.md`: Release checklist and device QA matrix
- `docs/11-phase-5-launch-hardening.md`: Phase 5 launch hardening plan
- `docs/12-uat-script.md`: End-to-end UAT sign-off script

## MVP Scope

- User authentication
- Project creation and management
- Room scan session storage
- Wall and floor material application
- Furniture placement in AR
- Save/share design snapshots
- Basic cost estimation

## Next Step

Start with `docs/04-sprint-01-backlog.md` and create tickets in Jira/ClickUp exactly as listed.
Then execute app environment setup from `docs/05-firebase-app-setup.md`.

## Roadmap

- [ ] Complete Android AR placement and persistence on supported ARCore devices
- [ ] Complete iOS RoomPlan scan review and editable floor-plan flow
- [ ] Add secure Firebase Auth onboarding and per-user data isolation tests
- [ ] Add collaborative project comments and design-version conflict handling
- [ ] Improve material previews, furniture catalog search, and saved favorites
- [ ] Add presentation boards and polished PDF estimates
- [ ] Add offline-first caching and sync recovery states
- [ ] Add Android/iOS automated tests and CI checks
- [ ] Run device-matrix QA and publish closed TestFlight/Play betas
- [ ] Add accessibility, localization, and privacy-review sign-off

## Security and privacy

Firebase configuration files, local SDK paths, logs, and generated builds are ignored because they may contain identifiers, keys, or machine-specific data. Use Firebase Security Rules and server-side validation for all user-owned projects. Review `firestore.rules` before deploying.

## Support the project

If AR Home Design helps you plan a better space, support continued development with a coffee:

<p align="center">
  <a href="https://buymeacoffee.com/awaisdev572">
    <img src="https://img.buymeacoffee.com/button-api/?text=Buy%20me%20a%20coffee&emoji=%E2%98%95&slug=awaisdev572&button_colour=FFDD00&font_colour=000000&font_family=Cookie&outline_colour=000000&coffee_colour=ffffff" alt="Buy me a coffee" height="60">
  </a>
</p>

Contributions help fund AR device testing, cloud infrastructure, accessibility improvements, and future releases.

## Contributing

Create a focused branch, keep credentials out of commits, update relevant docs, and test the affected platform before opening a pull request.

## License

No license has been selected yet. Until one is added, all rights are reserved by the project owner.
