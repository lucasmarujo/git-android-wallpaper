# Automated release: Google Play and App Store

Workflow: [`.github/workflows/release.yml`](../workflows/release.yml).

## Flow

| Event | Android | iOS |
|---|---|---|
| push to `main` touching `app/`, the Gradle files, `ios/` or `.eas/` | signed AAB → Google Play internal testing | EAS build → TestFlight |
| tag `vX.Y.Z` | signed AAB → Google Play production, with release notes, and a GitHub release | EAS build → TestFlight → submitted for App Store review (released automatically once approved) |
| pull request touching the pipeline | workflow lint, script tests and a signed AAB as an artifact, nothing published | — |
| `workflow_dispatch` | signed AAB as an artifact; publishes to internal testing only with `publish` checked | same, TestFlight |

The app CI (`testDebugUnitTest`) runs before any build. If it fails, nothing is published.

## Version

- **versionName / version**: `versionName` in `app/build.gradle.kts` and `MARKETING_VERSION` in `ios/project.yml`; on a tag both must match. The tag must match (`v1.2.0`), or the workflow fails
  before building. Flow: raise the version, merge, create the tag.
- **versionCode (Android)**: `1000 + run number` of this workflow, applied at build time by
  `android-version-code.gradle`. No manual edits. Do not rename the workflow: the run number restarts.
- **buildNumber (iOS)**: CI writes `1000 + run number` to `CURRENT_PROJECT_VERSION` in the Xcode project before `eas build`.
- **Notes**: the message of an annotated tag (`git tag -a v1.2.0 -m "..."`); without one, the
  commits since the previous tag (up to 500 characters). Listing language: `en-US`.

## Secrets

| Secret | Content | Status on 2026-11-14 |
|---|---|---|
| `ANDROID_KEYSTORE_BASE64` | base64 upload keystore | set |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password | set |
| `ANDROID_KEY_ALIAS` | key alias | set |
| `ANDROID_KEY_PASSWORD` | key password | set |
| `PLAY_SERVICE_ACCOUNT_JSON` | service account JSON with release permission on the app (Play Console) | **missing** |
| `EXPO_TOKEN` | access token of the Expo account that owns the project | **missing** |
| `APP_STORE_CONNECT_API_KEY_BASE64` | base64 App Store Connect API `.p8` key | set |
| `APP_STORE_CONNECT_API_KEY_ID` | key ID | set |
| `APP_STORE_CONNECT_API_ISSUER_ID` | team issuer ID | set |

Without `PLAY_SERVICE_ACCOUNT_JSON` the AAB is built and kept as an artifact, with a warning. Without the
iOS secrets the iOS job is skipped, with a warning.

## Release

```bash
# internal testing and TestFlight: merging into main is enough
git tag -a v1.2.0 -m "What changed in this version"
git push origin v1.2.0
```

A push builds only the platforms it touched. The tag release used to live in `android.yml`; it moved here
and now ships the signed APK and AAB. The old `SIGNING_*` secrets are no longer used.

Google Play production needs production access first (personal accounts: 12 testers for 14 days in closed
testing).
