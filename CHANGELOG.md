# 0.1.5 Android / 0.2.6 Windows preview — 2026-10-08

- Moves the project to **Maddex712/fnaf-redone**, preserving the complete local Git history.
- Updates both apps' GitHub links, release feeds, and verified download allowlists.
- Includes all earlier changes, including the Android updater and cached version label.
- Rebuilds the Android APK, Windows portable client, and FreddyFiles installer.
- Existing builds pointing to the old account need one manual update. Install the APK
  over the existing Android app, or select the existing Windows game folder in
  FreddyFiles. Keep existing saves and imported Steam data; do not uninstall first.

# 0.1.4 Android preview — prepared 2026-10-06

- Adds **Options → Check for updates** and a visible installed version.
- Finds Android releases separately from Windows, downloads APKs with progress
  and cancellation, and verifies their checksum, package, version, and signing identity.
- Opens Android's install-source permission page when needed, then its normal
  update confirmation. The existing package and signing identity are unchanged.
- Keeps a verified download for retry after cancelling the system installer;
  removes it after a successful update. Private saves and Steam imports stay in place.
- Adds connection/unavailable-release messages and a release-notes link.
- Shared gameplay and Windows behavior remain unchanged. Android 8.0+ remains supported.
- Local verification: 23 update checks, emulator APK/provider checks, and a real
  Android installer upgrade preserving private test data/preferences. Public live
  download verification and publication are blocked by GitHub account suspension.

# 0.2.5 Windows preview — 2026-10-06

- Adds **freddyfiles.exe**, a per-user installer with Start menu and optional desktop
  shortcuts, a stable game path, and an uninstaller that survives in-game updates.
  The portable ZIP remains available; the game payload is unchanged.
- Adds **Check for updates → Install and restart**. Downloads the Windows release,
  verifies GitHub's SHA-256 digest and archive structure, and stages the full app.
- Replaces the application at its existing path after shutdown and relaunches the
  same EXE, keeping WinHanced entries and shortcuts valid.
- Preserves external saves/imported Steam data, keeps one previous-version backup,
  and rolls back failed folder replacement. Downloads can be cancelled.
- Versions 0.2.4 and earlier need one manual update into the existing folder to
  install this updater; future updates use the in-app flow.
- Retains the 1080p performance changes the OXP3 owner reported working flawlessly.
- Android remains on 0.1.3 preview.

# 0.2.4 Windows preview / 0.1.3 Android preview — 2026-10-06

- Windows defaults to 1080p composition, including on HiDPI displays. The previous
  default 720p mode now becomes 1080p; 720p and Native remain optional.
- Caches resized artwork independently of camera position, with a bounded cache.
  In a paired local 1080p draw benchmark, camera median fell from 32.63 ms to 7.96 ms.
  This is a rendering measurement on the development PC, not an OXP3 FPS claim.
- Moves Windows native audio start/pause/volume/stop/close off the game thread.
- Uses the latest analog sample per frame without losing digital button edges;
  adds menu-stick hysteresis and controlled repeats.
- Extends the Windows report with update intervals, composition resolution,
  Windows scale, and current stick/button state.
- Adds a controller- and touch/click-accessible GitHub project link and a discreet
  Scott Cawthon attribution in Settings on both Android and Windows.
- Android keeps its package/signing identity and existing gameplay/rendering.
- OXP3 hardware verification remains pending; Intel-specific fault is not established.

# 0.2.3 Windows preview — 2026-10-06

- Responds to continuing OXP3 slowdown with a default Handheld rendering mode:
  bounded 720p composition and fast scaling instead of the old filtered fallback.
- Adds saved Smooth and Native rendering alternatives in Options.
- Moves native XInput queries off the game/UI thread, preserves ordered button
  edges, and discards queued input when focus is lost.
- Avoids redundant native audio-volume changes each frame.
- Adds a copyable performance/controller report with recent gameplay timings.
- Controller-reader, audio, display/update, scheduling, and integration checks pass.
  OXP3 hardware performance still needs verification; this release does not claim it is resolved.
- Android is unchanged.

# 0.2.2 Windows preview — 2026-10-06

- Fixes frame pacing with monotonic 60 Hz deadlines and bounded update scheduling.
- Polls the controller before simulation; idle sticks no longer erase keyboard look.
- Adds premultiplied image caching, reusable transition layers, and a 720p fallback
  for sustained slow drawing on high-resolution or unaccelerated displays.
- Detects display ratio at startup and on window changes, with proportional title-menu fitting.
- Adds the installed version and display information to Options.
- Adds a manual GitHub update check that selects Windows releases and opens their download page.
- Passes 35 desktop integration checks, 31 display/update checks, scheduling tests,
  and all shared gameplay assertions. OXP3 hardware verification remains pending.
- Android remains on 0.1.2 preview.

# 0.2.1 Windows preview — 2026-10-06

- Adds background Steam-library scanning and automatic import of a detected FNaF 1 copy.
- Finds additional Steam libraries and custom installation folders through Steam metadata.
- Offers a choice for multiple installations and a manual file-picker fallback.
- Adds Scan Steam and Browse File buttons to Windows setup; reimport remains in Options.
- Passes 11 discovery checks and 27 desktop integration checks, including real scan-to-import.
- Android remains on 0.1.2 preview.

# 0.2.0 Windows preview — 2026-10-06

- Adds a portable Windows x64 client with a bundled Java runtime.
- Shares gameplay, menus, progression, asset import, and visual-effect code with Android.
- Adds XInput controls, keyboard/mouse input, Windows file selection, fullscreen toggle,
  local Windows saves, and optional second-monitor support.
- Handles controller disconnection, focus loss, audio pause/resume, and HiDPI hit boxes.
- Android remains on the unchanged 0.1.2 preview. Physical OXP3 verification is pending.

# 0.1.2 preview — 2026-10-06

- Renames the project and Android launcher label to **FNaF Redone**.
- Updates the repository, banner, documentation, and release download names.
- Keeps `dev.thor.fnaf.preview` and the signing identity, so installing this update
  over the previous public preview preserves saves and imported Steam data.
- Gameplay and single-/dual-screen behavior are unchanged.

# 0.1.1 preview — 2026-10-06

- Packages the latest FNaF 1 local project as a separately maintained public build.
- Adds direct, offline import of the verified Steam executable on Android.
- Retains compatibility with existing prepared Steam data ZIPs.
- Adds first-run setup instructions, controller-accessible import and clear results.
- Keeps the approved title art, death sequence, shop and office plushies.
- Adds optional local font import; removes the bundled Windows font.
- Builds without Android's debuggable flag. The GitHub preview installs as
  `dev.thor.fnaf.preview`, alongside the older private app, with separate saves.
- Includes source, build instructions, provenance notes and checksums.
- Adds saved Auto / Single screen / Dual screen modes, with full-screen office and
  camera switching and a dedicated Shift Dashboard page in settings.
- Removes the split-screen fallback; single-screen jumpscares, power failures,
  intros and endings use the main viewport. The companion death vignette is omitted
  in single-screen mode; rewards appear below the main 6 AM celebration.

Gameplay logic is unchanged from the latest local FNaF 1 revision. This release
does not make a new claim of exact parity with the original game's executable.
