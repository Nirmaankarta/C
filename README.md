# Priority — Eisenhower Matrix (native Android, Kotlin + Jetpack Compose)

Zero third-party libraries (only AndroidX/Compose). Procedural sound, drawn icons, SharedPreferences storage.

## Build the APK
**Android Studio:** File > Open this folder > Run (or Build > Build APK).
**Command line:** `./gradlew assembleRelease` -> `app/build/outputs/apk/release/app-release.apk` (debug-signed, sideloadable).
**GitHub:** push this folder; Actions > "Build APK" produces the APK as a downloadable artifact.

## Layout
- Phone (<600dp): All/Q1-Q4 tabs, bottom-centre FAB, slide-up sheet, per-card quadrant mover.
- Tablet / large screen: 2x2 grid, centre "+" at the crossroads, long-press & drag cards between quadrants.

## Files
Model.kt, Store.kt, MatrixViewModel.kt (tasks, streak, profile) · Sfx.kt (8 synthesised sounds) ·
Visuals.kt (theme, icons, avatars) · MatrixScreen.kt · Sheets.kt (add task, profile/settings) · MainActivity.kt
