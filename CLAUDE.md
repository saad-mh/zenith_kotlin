# Zenith contributor notes

Android app in Kotlin with Jetpack Compose, Material 3, Room, and Hilt. One `app` module, minimum SDK 31.

## Build and test

```powershell
.\gradlew.bat assembleDebug            # debug APK
.\gradlew.bat testDebugUnitTest        # JVM unit tests
.\gradlew.bat connectedDebugAndroidTest  # instrumented tests, needs a device or emulator
```

On macOS or Linux, use `./gradlew` in place of `.\gradlew.bat`.

## Conventions

- Add dependency versions to `gradle/libs.versions.toml`, not inline in the build scripts.
- The v2 plan is in `docs/v2/PLAN.md`; work is tracked as GitHub issues under the v2.0 milestone.
