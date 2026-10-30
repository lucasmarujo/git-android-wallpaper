<div align="center">

<img src="docs/images/icon.png" alt="Git Wallpaper icon" width="112" />

# Git Wallpaper

**Your GitHub contribution graph as your Android lock screen wallpaper, always up to date.**

[![Android CI](https://github.com/lucasmarujo/git-android-wallpaper/actions/workflows/android.yml/badge.svg)](https://github.com/lucasmarujo/git-android-wallpaper/actions/workflows/android.yml)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin 2.4](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Google Play: closed testing](https://img.shields.io/badge/Google%20Play-closed%20testing-414141?logo=googleplay&logoColor=white)
[![License: MIT](https://img.shields.io/badge/License-MIT-39D353.svg)](LICENSE)

</div>

Git Wallpaper is a native Android app that draws your GitHub contribution graph as a wallpaper for the lock screen, the home screen, or both, and as a home screen widget you can resize to any size. Both stay in sync with your commits in the background. Everything runs on the device: there is no backend, no analytics and no ads. The app talks only to GitHub.

<table>
  <tr>
    <td align="center"><img src="docs/images/lock-screen.png" alt="Lock screen with the contribution graph" width="220" /><br /><sub>Lock screen</sub></td>
    <td align="center"><img src="docs/images/home-screen.png" alt="Home screen with the same wallpaper" width="220" /><br /><sub>Home screen</sub></td>
    <td align="center"><img src="docs/images/app-home.png" alt="Main screen of the app" width="220" /><br /><sub>Preview and stats</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/images/adjust-framing.png" alt="Full-screen framing editor" width="220" /><br /><sub>Pinch to zoom, drag to position</sub></td>
    <td align="center"><img src="docs/images/commit-color.png" alt="RGB commit color picker" width="220" /><br /><sub>Any commit color</sub></td>
    <td align="center"><img src="docs/images/sign-in.png" alt="GitHub device flow sign-in" width="220" /><br /><sub>Sign in with GitHub</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/images/widget-strip.png" alt="Home screen widget at 4 by 1 with GitHub's week columns" width="220" /><br /><sub>Widget 4×1: GitHub's week columns</sub></td>
    <td align="center"><img src="docs/images/widget-medium.png" alt="Home screen widget at 4 by 2 with the wallpaper day grid" width="220" /><br /><sub>Widget 4×2: the wallpaper grid</sub></td>
    <td align="center"><img src="docs/images/widget-tall.png" alt="Home screen widget stretched to 4 by 4" width="220" /><br /><sub>Widget stretched to 4×4</sub></td>
  </tr>
</table>

<sub>Screenshots show the Brazilian Portuguese interface. The app is also fully available in English.</sub>

## Contents

- [Features](#features)
- [How it works](#how-it-works)
- [Installation](#installation)
- [iPhone app](#iphone-app)
- [Privacy and security](#privacy-and-security)
- [Troubleshooting](#troubleshooting)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Building from source](#building-from-source)
- [Testing](#testing)
- [Continuous integration and releases](#continuous-integration-and-releases)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Disclaimer](#disclaimer)

## Features

### A wallpaper made from your year

- **One square per day** of the current year or of the last 12 months, laid out as a vertical grid that fits phone screens. The number of columns adapts to the screen's aspect ratio.
- **Today is ringed**, and future days are dimmed, so you can see at a glance where you are in the year.
- **An optional footer** shows the total contributions, the current and longest streaks, your avatar and your `@username`.
- **Rendered on the device at the screen's physical resolution.** The grid stays sharp on every display.
- **Lock screen safe zones.** By default the content sits between the clock and the unlock or shortcut area, and it already compensates for the roughly 1.1× zoom Android applies to wallpapers.

### Adjustable framing (new in 1.1)

- **A lock screen preview before every apply.** Tapping **Apply wallpaper** opens a full-screen preview of the lock screen, with a simulated clock.
- **Gestures:** pinch to zoom (0.5× to 2.5×) and drag to reposition the graph. **Reset** restores the default framing.
- **The framing persists.** It is saved and reused by every automatic update, so the wallpaper never jumps back to the default position.

### Home screen widget (new in 1.2)

- **Add it your way.** Tap **Add widget** in the app and confirm the launcher's dialog, or touch and hold the home screen, open **Widgets** and drag **Git Wallpaper** wherever you like.
- **Any size.** The widget resizes freely in both directions, from a single cell to the whole screen, and the graph never wraps:
  - **short and wide** (about one launcher row, the default 4×1): GitHub's familiar graph, one column per week. Stretch it sideways to see more weeks, always ending at the current one;
  - **stretched vertically:** the same grid as the wallpaper, one square per day from left to right and top to bottom, with the number of columns chosen to fill the widget.
  - Very small widgets show the most recent days that still fit at a readable size.
- **Same look as your wallpaper.** It follows the theme, commit color, period, cell shape and stats you picked in the app. A header with the total contributions appears when there is room and stats are on.
- **Always fresh.** The same background job that updates the wallpaper redraws every widget, even if you never applied the wallpaper. Tapping the widget opens the app.
- **Native corners.** On Android 12 and later it uses the launcher's widget corner radius.

### Personalization

| Setting | Options |
| --- | --- |
| Apply to | Lock screen, home screen, or both |
| Theme | Dark (GitHub), Light, AMOLED, Ocean, Sunset |
| Commit color | The theme's colors, or any color from an RGB picker with live preview and hex code. The four contribution levels are derived from the chosen color, while the theme keeps the background and text. |
| Period | This year, or the last 12 months |
| Cell shape | Rounded squares or circles |
| Stats footer | On or off (also shows the total on the widget) |
| Home screen widget | Any size, added from the app or from the system widget list |
| Automatic update | On or off, checking GitHub every 30 min, 1 h, 3 h, 6 h or 12 h |

### Hands-off updates

- **Background updates through WorkManager.** There is one unique periodic job, with no network constraint and exponential backoff.
- **When it redraws:** after a new contribution, and at the turn of each day so the "today" ring moves, even offline using the cached calendar.
- **Widgets included.** Each run also redraws the home screen widgets. Updates are scheduled while the wallpaper is applied or at least one widget is on the home screen, and they never set a wallpaper you did not apply.
- **It keeps running** after a reboot or an app update.
- **No redundant redraws.** Each run compares a SHA-256 fingerprint of everything that affects the pixels (calendar, settings, framing, color, screen size, locale and date) and skips identical renders.
- **Battery-optimization shortcut.** A built-in shortcut opens the system settings so Android does not defer updates. The app does not request the exemption directly, in line with Google Play policy.

### Sign-in

- **GitHub OAuth Device Flow.** No backend and no client secret are involved. The app shows the verification link with **Open link** and **Copy link** buttons, plus a one-tap **Copy code and open GitHub** action.
- **Fallback:** a Personal Access Token with the `read:user` scope.

### Localization

The app is available in English and Brazilian Portuguese, with per-app language support on Android 13 and later.

## How it works

1. **Sign in** with GitHub. Open the verification link, enter the code shown in the app and authorize it, on the phone or on any other device.
2. **Pick your style:** where to apply it, theme, commit color, period, cell shape and whether to show stats. The preview updates as you change options.
3. **Tap Apply wallpaper**, adjust the zoom and position on the lock screen preview, and confirm.
4. **Want it on the home screen too?** Tap **Add widget**, or add it from the system widget list, and resize it as you like.
5. **That's it.** The app checks GitHub on the interval you chose and redraws the wallpaper and the widgets whenever something changed. You do not need to open it again.

Under the hood, each update reads your contribution calendar through the GitHub GraphQL API (`viewer { login avatarUrl contributionsCollection { contributionCalendar } }`), renders a bitmap with Android's `Canvas`, and applies it with `WallpaperManager.setBitmap` using `FLAG_LOCK` and/or `FLAG_SYSTEM`. Widgets are drawn by the same renderer at each widget's own size and delivered through `AppWidgetManager` with `RemoteViews`.

## Installation

- **Google Play:** the app (`dev.marujo.gitwallpaper`) is currently in closed testing. If you are on the tester list, join through the [testing page](https://play.google.com/apps/testing/dev.marujo.gitwallpaper) and install it from Google Play. A public listing will follow after the testing period.
- **GitHub Releases:** tagging a version (`v*`) publishes a signed APK to [Releases](../../releases). Installing it requires allowing installs from unknown sources.
- **From source:** see [Building from source](#building-from-source).

**Requirements:** Android 8.0 (API 26) or later, and a GitHub account.

## iPhone app

A native iPhone version lives in [`ios/`](ios), written in Swift and SwiftUI. The Android app is not affected by it. It uses the same rendering rules, themes, colors, framing, stats and settings, and it is available in English and Brazilian Portuguese.

**What changes on iOS.** iOS has no public API that lets an app change the wallpaper by itself, so the iPhone app sets it through the Shortcuts app:

1. The app provides a **Get Git Wallpaper** action. It refreshes your contributions and returns the wallpaper image, drawn at the screen's native resolution with your framing.
2. You create a shortcut named **Git Wallpaper** once: **Get Git Wallpaper**, then **Set Wallpaper Photo** with Lock Screen, Home Screen or both. After that, **Apply wallpaper** in the app runs it for you, through `x-callback-url`.
3. A **Time of Day** automation set to **Run Immediately** keeps the wallpaper up to date without opening the app. The app shows these steps the first time you apply.

You can also save the image to Photos and choose it in **Settings > Wallpaper**.

**Widgets.** The Home Screen widget comes in small, medium and large sizes, and there is a rectangular Lock Screen widget. They use the same layout rules as the Android widget: the Lock Screen widget shows GitHub's week columns, and the larger ones show the wallpaper's day grid. WidgetKit refreshes them on the interval chosen in the app and moves the "today" ring at midnight.

**Security.** The token is stored in the iOS Keychain (this device only) and shared only with the widget through the `group.dev.marujo.gitwallpaper` App Group. Caches are excluded from backups.

| Area | Choice |
| --- | --- |
| Language and UI | Swift 5 language mode, SwiftUI, iOS 17 or later, iPhone |
| Shared code | `GitWallpaperKit` local Swift package: domain, GitHub, storage, Core Graphics renderer |
| Widgets and automation | WidgetKit, App Intents (Shortcuts) |
| Project | [XcodeGen](https://github.com/yonaskolb/XcodeGen) spec in [`ios/project.yml`](ios/project.yml), with the generated `ios/GitWallpaper.xcodeproj` committed |
| Store builds | EAS Build and EAS Submit (TestFlight) |
| Tests | XCTest, with `URLProtocol` stubs for GitHub and pixel checks for the renderer |

**Building on a Mac** (Xcode 26 or later). After changing `ios/project.yml` or adding files, run `brew install xcodegen && xcodegen generate` in `ios/`:

```bash
cd ios
open GitWallpaper.xcodeproj
```

Tests: `xcodebuild test -scheme GitWallpaperKit -destination 'platform=iOS Simulator,name=iPhone 17'` inside `ios/GitWallpaperKit`, and `xcodebuild test -project GitWallpaper.xcodeproj -scheme GitWallpaper -destination 'platform=iOS Simulator,name=iPhone 17'` inside `ios`.

## Privacy and security

- **On-device only.** There is no server, analytics, tracking, ads or third-party SDKs. No data is sent to the developer.
- **Encrypted token.** The GitHub token is encrypted with AES-256-GCM using a key held in the Android Keystore, and it is sent only to GitHub over HTTPS.
- **Minimal scope:** `read:user`.
- **Network access is limited to GitHub:**
  - `github.com` for sign-in;
  - `api.github.com` for contributions;
  - `avatars.githubusercontent.com` for your avatar.
- **Backups and cleartext are off.** App data is excluded from cloud backup and device-to-device transfer, and cleartext traffic is disabled.
- **Sign out** deletes the token and the contributions cache, and stops automatic updates. You can also revoke access at any time at [github.com/settings/applications](https://github.com/settings/applications).
- **Privacy policy:** [lucasmarujo.github.io/git-android-wallpaper/privacy](https://lucasmarujo.github.io/git-android-wallpaper/privacy/). The source is in [`docs/privacy/index.md`](docs/privacy/index.md), published with GitHub Pages.

**Permissions**

| Permission | Why |
| --- | --- |
| `INTERNET` | Talk to GitHub |
| `SET_WALLPAPER` | Apply the generated image |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` | Added by WorkManager to run and reschedule background updates |

## Troubleshooting

<details>
<summary><strong>My private contributions are missing</strong></summary>

Enable **Private contributions** in your GitHub profile settings. GitHub then includes them, anonymized, in your contribution calendar.
</details>

<details>
<summary><strong>The wallpaper stops updating on its own</strong></summary>

Some manufacturers (for example Xiaomi, Samsung and Huawei) aggressively restrict background work. When the app shows the battery hint, tap **Open battery settings** and set Git Wallpaper to **Don't optimize** (or **Unrestricted**). If you **force stop** the app, Android cancels its scheduled work until you open it again. This is a system rule.
</details>

<details>
<summary><strong>The clock covers part of the graph</strong></summary>

Lock screen clocks differ across Android versions and manufacturers. Tap **Apply wallpaper** and drag or zoom the graph in the preview until it sits where you like. The framing is kept for all future updates.
</details>

<details>
<summary><strong>The graph looks slightly smaller or larger than in the preview</strong></summary>

The safe zones assume AOSP's default wallpaper zoom of about 1.1×. On devices that do not zoom the wallpaper, the content appears slightly more compact. Adjust it once in the framing editor.
</details>

<details>
<summary><strong>"GitHub sign-in is unavailable"</strong></summary>

The build has no OAuth Client ID configured, or GitHub rejected the Device Flow request. Use **Use a personal access token** with the `read:user` scope instead.
</details>

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.4 (JVM 17 bytecode, built with JDK 21) |
| UI | Jetpack Compose, Material 3 (Compose BOM 2026.09) |
| Background work | WorkManager 2.12 |
| Storage | DataStore Preferences 1.3 |
| Networking | OkHttp 5 with kotlinx.serialization (GitHub GraphQL) |
| Security | Android Keystore with AES-256-GCM |
| Build | Android Gradle Plugin 9.4, Gradle 9.8, version catalog |
| SDK levels | `compileSdk 37`, `targetSdk 36`, `minSdk 26` |
| Tests | JUnit 4, Robolectric 4.17 (native graphics), Compose UI tests, kotlinx-coroutines-test, MockWebServer, WorkManager testing |

## Architecture

A single `app` module, organized in layers with manual dependency injection through `AppContainer`. The same renderer produces the in-app preview, the real wallpaper and the home screen widget, so what you see is what gets applied.

```mermaid
flowchart LR
    subgraph UI["ui (Compose)"]
        Login[LoginScreen] --> LoginVM[LoginViewModel]
        Home[HomeScreen / WallpaperAdjustScreen] --> HomeVM[HomeViewModel]
    end
    subgraph Data["data"]
        Auth[AuthRepository + DeviceFlowClient]
        Session[SessionRepository<br/>Keystore-encrypted token]
        Contrib[ContributionRepository<br/>GitHub GraphQL + cache]
        Settings[SettingsRepository<br/>DataStore]
    end
    subgraph Wallpaper["wallpaper"]
        Updater[DefaultWallpaperUpdater<br/>fingerprint check]
        Renderer[ContributionWallpaperRenderer<br/>Canvas]
        Applier[SystemWallpaperApplier<br/>WallpaperManager]
    end
    subgraph Widget["widget"]
        Provider[ContributionWidgetProvider<br/>AppWidgetProvider]
        WidgetUpd[ContributionWidgetUpdater<br/>RemoteViews]
    end
    Worker[WallpaperUpdateWorker<br/>WorkManager] --> Updater
    Worker --> WidgetUpd
    Provider --> WidgetUpd
    HomeVM --> WidgetUpd
    WidgetUpd --> Contrib
    WidgetUpd --> Renderer
    LoginVM --> Auth --> Session
    HomeVM --> Settings
    HomeVM --> Contrib
    HomeVM --> Updater
    HomeVM --> Renderer
    Updater --> Contrib
    Updater --> Settings
    Updater --> Renderer --> Applier
```

```
app/src/main/java/com/lucasmarujo/gitwallpaper
├── domain/      Pure models and rules: calendar, stats, settings, framing, periods, themes
├── data/        GitHub GraphQL, avatar cache, Device Flow, encrypted session, DataStore
├── wallpaper/   Day grid, wallpaper and widget layouts, Canvas renderer, palettes, fingerprint, applier
├── widget/      Home screen widget provider and RemoteViews updater
├── work/        WorkManager worker, factory, scheduler and the auto-update rule
└── ui/          Compose screens (login, home, framing editor, color picker) and theme
```

Design and planning notes, in Portuguese, live in [`docs/PLANEJAMENTO.md`](docs/PLANEJAMENTO.md).

## Building from source

**Prerequisites:** JDK 21 and the Android SDK with platform 37 (Android Studio installs both).

```bash
git clone https://github.com/lucasmarujo/git-android-wallpaper.git
cd git-android-wallpaper
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # install on a connected device or emulator
```

### GitHub OAuth App (Device Flow)

The OAuth Client ID is read from `gradle.properties` (`gitWallpaper.githubClientId`). It is not a secret, because the Device Flow uses no client secret. To use your own OAuth App:

1. Create an OAuth App at <https://github.com/settings/applications/new> and check **Enable Device Flow**.
2. Replace `gitWallpaper.githubClientId`, or pass it at build time: `./gradlew assembleDebug -PgitWallpaper.githubClientId=YOUR_CLIENT_ID`.

Without a Client ID the app still works, with Personal Access Token sign-in only.

### Signed release builds

Release builds are minified and resource-shrunk. They are signed when a `keystore.properties` file exists at the project root. The file is git-ignored.

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=git-wallpaper
keyPassword=...
```

Each value can also come from the `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD` environment variables.

```bash
./gradlew assembleRelease      # signed APK
./gradlew bundleRelease        # signed Android App Bundle for Google Play
```

Google Play distribution uses Play App Signing: the local keystore is the upload key.

## Testing

The project has 233 unit tests that run on the JVM, using Robolectric for Android APIs and native graphics.

```bash
./gradlew testDebugUnitTest    # unit and Robolectric tests
./gradlew lintDebug            # Android Lint
```

The tests cover:

- **Domain:** stats and streaks, the day grid (current year, leap years, last 12 months, GitHub's timezone ahead of the device), vertical layout and safe zones, framing limits, fingerprints.
- **Rendering with real pixels:** bitmap size, theme backgrounds, colors per contribution level, future days, the today ring, circles versus squares, the stats footer, the avatar, custom commit colors, zoom and position.
- **Widget:**
  - the layout at any size and density: GitHub's week columns when short, the wallpaper's day grid when stretched vertically, no wrapping, and the current week always visible;
  - widget rendering: rounded corners, colors, the today ring and the header;
  - real `RemoteViews` updates: each widget at its own size, the empty state, opening the app on tap, and a bounded bitmap for huge widgets;
  - pinning from the app, and the provider metadata (resizable in both directions from one cell).
- **Data:** GraphQL parsing and errors, Device Flow polling, token encryption and session lifecycle, avatar cache, settings persistence and migration of stored values.
- **Orchestration:**
  - the updater: apply, skip when unchanged, force, a new day online and offline, cache fallback, revoked tokens, wallpaper failures, framing kept by automatic updates, no wallpaper set before the first apply;
  - the worker's result mapping and widget redraws;
  - when automatic updates are scheduled (applied wallpaper, widgets, or neither);
  - unique scheduling in WorkManager.
- **UI (Compose):** sign-in states, open and copy link actions, settings, the RGB color picker, the framing editor's gestures (pinch, drag, reset, cancel) and the status bar appearance.

## Continuous integration and releases

[GitHub Actions](.github/workflows/android.yml) runs on every push to `main` and on every pull request:

1. Unit tests, lint and a debug build on JDK 21.
2. Uploads the test and lint reports and the debug APK as artifacts.

[iOS CI](.github/workflows/ios.yml) runs the iOS package and app tests on macOS 26 with Xcode 26.6 whenever `ios/` changes. It also fails when the committed `ios/GitWallpaper.xcodeproj` no longer matches `ios/project.yml`.

iOS store builds go through [EAS Build](https://docs.expo.dev/build/introduction/), which works from any OS, including Windows. The root `package.json`, `app.json` and `eas.json` only exist for EAS, and [`.eas/build/ios-app-store.yml`](.eas/build/ios-app-store.yml) builds the native Xcode project without React Native. EAS manages the distribution certificate and the provisioning profiles of the app and the widget, and EAS Submit uploads the build to TestFlight:

```bash
eas build --platform ios --profile production --auto-submit
```

Raise `MARKETING_VERSION` or `CURRENT_PROJECT_VERSION` in `ios/project.yml` before each upload, then regenerate the Xcode project on a Mac (or take it from the CI artifact).

Pushing a tag that matches `v*` also builds a signed release APK and publishes it as a GitHub Release with generated notes. That job needs the `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD` repository secrets.

```bash
git tag v1.2.0
git push origin v1.2.0
```

## Roadmap

- Foldable support, with a separate render per display.
- Custom backgrounds, and a theme that follows the system's light or dark mode.
- Public release on Google Play after the closed testing period.

## Contributing

Issues and pull requests are welcome.

1. Open an issue that describes the bug or the proposal.
2. Keep changes focused and consistent with the existing architecture and code style.
3. Add or update tests for every change.
4. Make sure `./gradlew testDebugUnitTest lintDebug` passes before opening the pull request.

By contributing, you agree that your contributions are licensed under the [MIT License](LICENSE).

## License

Git Wallpaper is open source software released under the [MIT License](LICENSE). Copyright (c) 2026 Lucas Marujo.

## Disclaimer

Git Wallpaper is an independent project published by Alt Software. It is not affiliated with, endorsed by or sponsored by GitHub, Inc. GitHub is a trademark of GitHub, Inc.
