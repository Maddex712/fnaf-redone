# Release verification — 2026-10-06

Version: 0.1.1-preview, Android version code 2, public preview package `dev.thor.fnaf.preview`.

## Completed checks

- Build and signature verification passed (APK signing schemes v2 and v3).
- Android package inspection confirms no debuggable flag. No network permission
  or Steam login is used.
- JVM suites passed: simulation 240, progress 47, intro 20, title effects 26,467,
  outro 4,848 and screen modes 20 checks.
- Direct Steam importer matched all 605 reference images pixel for pixel and
  all 52 reference WAVs byte for byte; unsupported executable rejection passed.
  Reference material remained outside the release source/archive.
- Fresh Android 35 x86_64 emulator: installed signed APK, selected the player's
  Steam executable through the real system document picker, completed extraction,
  reached Ready to play, and started Night 1. Verified 605 images and 52 sounds
  in the installed pack. No PC extraction tool was used for this path.
- 24 Android instrumentation checks passed on the final APK: update retained
  imported data, controller navigation, camera/office switching, pause, saved
  screen preference, single/dual virtual-display transitions, display removal,
  rejected EXE/unsafe ZIP recovery, temporary-file cleanup and unchanged save.
  The public build did not respond to either development shortcut.
- Optional font imported through the document picker; stored font SHA-256 matched
  the selected local file. Visually checked menu, dashboard, full-screen office
  and camera rendering. The Windows font is absent from the release APK/source.
- Source release lint: 0 failures, 0 warnings against the local Steam installation.
- Core `GameState.java` is byte-identical to the latest private FNaF 1 project.
  Packaging/screen-routing changes do not change AI or game difficulty rules.
- Temporary Android verification helper uninstalled after testing. It is not part
  of the APK or source download. No test save, points or game data are packaged.

## Limits

The public preview was subsequently installed on the physical AYN Thor as a
separate application. Its existing Steam data ZIP imported successfully, and both
menu screens were visually checked. The Steam EXE import also completed in
BlueStacks. The deeper automated screen-routing checks ran in the Android emulator. The emulator uses a virtual secondary display for dual-mode
checks. Single-screen phones/tablets and different Android file providers still
need physical-device testing. This pass does not certify every night or claim
frame-perfect parity with the original Windows game. Emulator audio was muted;
WAV integrity was checked, not a new recorded listening test.
