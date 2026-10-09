# Building and testing

Both apps live in one repository and share the same features, wording and test coverage. When you change one, make the same change in the other.

## Android

| | |
| --- | --- |
| Language | Kotlin 2.0.21, Jetpack Compose (BOM 2024.10.01) |
| Build | Gradle 8.14 wrapper, Android Gradle Plugin 8.6.0, JDK 17 |
| SDK | compileSdk 35, targetSdk 35, minSdk 26 (Android 8.0) |
| Application ID | `com.replaymac.companion` |
| Version | 1.0.0 (versionCode 1), set in `android/app/build.gradle.kts` |
| Dependencies | AndroidX Compose, Activity and Lifecycle, kotlinx coroutines and serialization, OkHttp 4.12.0 |

`android/gradle.properties` points `org.gradle.java.home` at `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home`. Change or remove that line if your JDK 17 lives somewhere else. Android Studio writes `local.properties` with your SDK path; it's gitignored.

Run everything from `android/`:

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release builds

```bash
./gradlew :app:assembleRelease
```

The APK lands in `app/build/outputs/apk/release/app-release.apk`. The release build is currently signed with the debug key and isn't minified, which is fine for sideloading but not for Google Play. To ship to Play, add a real `signingConfigs` block that reads a keystore. The `.gitignore` already excludes `*.jks`, `*.keystore`, `release-key.properties` and `signing.properties`, so keep those files out of the repo.

## iOS

| | |
| --- | --- |
| Language | Swift 5, SwiftUI, Combine; Apple frameworks only, no packages |
| Toolchain | Xcode 16 or later, [XcodeGen](https://github.com/yonaskolb/XcodeGen) |
| Deployment target | iOS 16.0, iPhone and iPad |
| Bundle ID | `com.replaycap.companion` |
| Version | 1.0.0 (build 1), set in `ios/ReplayMacCompanion/App/Info.plist` |

`ios/project.yml` is the source of truth for the Xcode project. Regenerate the project whenever you add, move or remove files:

```bash
cd ios
xcodegen generate
open ReplayCapCompanion.xcodeproj
```

Choose the **ReplayCapCompanion** scheme. Xcode creates it the first time you open the project, because neither `project.yml` nor the repo defines a shared scheme.

Signing is automatic with team `QZTBSUBN77`. To run on your own device, change the team in Signing & Capabilities (or in `project.yml`), and change the bundle identifier if the current one is taken.

### Tests

The tests are a hosted XCTest bundle, so they run in a simulator:

```bash
xcodebuild test -project ReplayCapCompanion.xcodeproj -scheme ReplayCapCompanion -destination 'platform=iOS Simulator,name=iPhone 16'
```

Swap `iPhone 16` for any simulator you have installed.

### What's called what

The iOS app was renamed from ReplayMac to ReplayCap, but the folders kept their old names:

| Thing | Name |
| --- | --- |
| Xcode project and app target | `ReplayCapCompanion` |
| Built app and display name | `ReplayCap.app`, shown as ReplayCap |
| Swift module (for `@testable import`) | `ReplayCapCompanion` |
| Source folder | `ios/ReplayMacCompanion/` |
| Test folder and target | `ios/ReplayMacCompanionTests/`, target `ReplayCapCompanionTests` |
| App entry point | `ReplayCapCompanionApp` in `App/ReplayMacCompanionApp.swift` |

## What the tests cover

Both platforms share the same core unit test suites, each with a few platform-specific tests on top:

- **Reconnect plan and loop**: the order addresses are tried in, the backoff sequence, and the auto-reconnector against a fake network (finding the Mac somewhere new, falling back to USB, stopping on denial). Both platforms cover this thoroughly.
- **Remembered Macs**: what's kept across network and USB connections, multi-Mac ordering, forgetting, and migration from the old single-Mac storage. Android covers `RememberedMacStore` directly, and iOS covers `RememberedMac`; both cover the model.
- **Protocol parsing**: every server message, including missing optional fields, unknown types and malformed JSON. Android covers `CompanionProtocolParser` (via `CompanionStateParserTest`), while iOS covers `CompanionProtocolParserTests` alongside its WebSocket client tests.
- **Client messages**: the JSON the phone sends for `hello`, actions and clip requests.
- **Home control labels**: tile order, titles, subtitles and enabled states for every state the Mac can be in.
- **Connection errors**: the plain-language message for each failure.
- **Formatting and connection logic**: durations, file sizes, clip types, telemetry merging and connection states.

Test coverage is currently unit-level across both platforms; UI and instrumented tests are planned for future updates.
