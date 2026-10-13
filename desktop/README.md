# FNaF Redone for Windows

A **Windows x64** client for handhelds such as the OXP3 and for desktop PCs.
This is the same gameplay and menu code used by the Android port, hosted by a
Windows Java2D/audio/input adapter. It does not run Android or emulate the APK.

## Play

1. Download [**freddyfiles.exe**](https://github.com/Maddex712/fnaf-redone-again/releases/download/v0.2.6-windows-preview/freddyfiles.exe).
2. Run FreddyFiles and select **Install**. It installs for your Windows account without administrator access.
3. Open **FNaF Redone** from its Start menu or desktop shortcut. No separate Java installation is needed.
4. Choose **New Game → Scan Steam** (A / Enter). The client finds your installed
   FNaF 1 copy and imports it. Use **Browse File** (X / Space) to select an EXE or
   existing data ZIP yourself.
5. Wait for **Ready to play** and start the game.

You need Windows 10/11 **64-bit**, your purchased Windows Steam FNaF 1 copy, and
at least **1.5 GB free** for import. The supported game file is the same as the
[Android build](../docs/COMPATIBILITY.md#supported-game-file). This preview is not
code-signed. Verify its SHA-256 against the release checksum file.

FreddyFiles installs to `%LOCALAPPDATA%\Programs\FNaF Redone` by default.
It uses Windows’ .NET Framework 4.8, included with current Windows 10/11 installations.
Launch the game from the shortcut created during installation.

Prefer no installer? The [portable ZIP](https://github.com/Maddex712/fnaf-redone-again/releases/tag/v0.2.6-windows-preview)
is still available. Extract the entire folder, keeping `app` and `runtime` beside
the EXE. The in-game updater uses this ZIP for both installation types.

To uninstall, use **Windows Settings → Apps → FNaF Redone → Uninstall** or the
Start menu uninstall shortcut. Saves and imported Steam files are kept in
`%LOCALAPPDATA%\FNaFRedone`. The small `<game-folder>.freddyfiles` maintenance
folder can be deleted after uninstall finishes; recovery copies are also kept
until you choose to remove them. Maintenance lives outside the application folder
so it continues working after the game's in-place updates.

The scan checks Steam's registered installation, standard locations, and additional
libraries listed in `libraryfolders.vdf`, including libraries on other drives.
It uses FNaF 1's app manifest to find custom installation folders. It runs in the
background and does not search your whole drive, launch Steam, or require a login.
If several copies are found, choose one; if none are found, the file picker opens.
The importer still validates the detected file before replacing existing game data.
Use **Options → Scan / import Steam copy** to scan again or browse manually.

## OXP3 controls

Use the handheld’s **Xbox / XInput gamepad mode**. If Steam Input or a desktop
controller mapper also sends keyboard/mouse events, use one mapping layer to avoid
duplicate actions. The physical OXP3 has not yet been tested by the maintainer.

The gamepad layout matches Android: left stick to look; L1/R1 doors; L2/R2 lights;
X monitor; D-pad cameras; Y mute; right-stick click nose; Start pause; A/B menus.
Use Options for button remapping, sensitivity, and the controller diagnostic page.
A controller disconnect pauses the night and releases held lights/stick input.

| Keyboard | Action |
| --- | --- |
| A / D | Look left / right |
| Q / E | Left / right door |
| Z / C | Hold left / right light |
| X or Space | Monitor |
| Arrow keys | Menus / camera selection |
| Enter / Backspace | Confirm / back |
| Escape | Pause / back |
| M | Mute phone |
| N | Freddy nose |
| F11 | Fullscreen / windowed |

Mouse clicks and drags use the shared touch controls. Windows touchscreen mouse
emulation is supported; this adapter does not yet implement independent simultaneous
multi-touch contacts. A gamepad is recommended for combined actions.

## Screens, extras, and saves

On every launch, the app detects the current monitor's resolution and aspect ratio.
It checks again when the window moves or resizes. Title menus and single-screen
gameplay fit a centered 16:9 viewport on 16:9, 16:10, 19:10, and other displays,
using black bars where needed to keep artwork proportional. **Options** shows the
detected display and installed version at the bottom.

**Options → Rendering** selects **1080p** (default), **720p**, or **Native**.
1080p composes at up to 1920×1080 physical pixels, accounting for Windows display
scaling. It reuses resized artwork while panning instead of resampling large images
every frame. Larger displays scale the completed frame; Native draws at the display
resolution. The selection is saved. Existing default Handheld settings now use
1080p; choose it explicitly if you previously selected a different mode.

Audio driver operations run off the game thread. Analog motion uses the newest
sample each frame, while button presses/releases remain ordered. Menu sticks have
a separate activation threshold and repeat delay to resist drift.

If performance is poor, play for a few seconds, then open **Options → Performance /
controller report → Copy report**. The report preserves recent gameplay frame-rate,
drawing, scaling, input, and audio-dispatch timings, along with update intervals,
Windows scaling, stick state, version, and display size.
Paste it into a bug report. It stays local unless you choose to share it.

**Options → GitHub project** opens this repository in your browser. Settings also
credits Scott Cawthon and identifies this as an unofficial fan project.

**Options → Check for updates → Install and restart** downloads the newest Windows
release, verifies its SHA-256, and replaces the application in its **current folder**.
Your existing shortcut continues to work.
The app restarts after replacement. Your saved progress, settings, and imported
Steam data remain in `%LOCALAPPDATA%\FNaFRedone`. Updating ends the current night.

Installation is opt-in and requires internet, built-in Windows PowerShell, and write
access to the application folder and its parent. Keep the portable app in a normal
user-writable folder. A failed download leaves the installed game unchanged. The
updater stages the full app, waits for the game to exit, then swaps folders. If the
swap fails, it restores the previous app. One recovery copy is kept beside it as
`<app-folder>.previous`; the next successful update replaces that recovery copy.
Helper logs are under `%LOCALAPPDATA%\FNaFRedone\updates`. If an interrupted update
leaves the app folder missing, rename the `.previous` folder back to its original name.

This remains a manual update check; playing and importing Steam files work offline.
Android updating is unchanged.

Single screen is the Windows default. Office and cameras use the full viewport.
Achievements, challenges, plushies, and the shop remain in the main menu.
**Options → Shift dashboard** exposes the companion terminal on one display.
Auto / Dual screen can use a second Windows monitor; physical dual-monitor behavior
still needs broader testing. The lower-screen death vignette appears only in dual mode.

The app starts fullscreen; press F11 to toggle. Losing focus pauses a running night
and freezes intro/outro timers until the game is focused again.

Data is stored in `%LOCALAPPDATA%\FNaFRedone`, separately from the app folder.
Replacing the extracted application folder preserves saves and imported assets.
Removing that data directory erases progress. There is no cross-device or Steam-save sync.
Consolas is used when installed on Windows; no Windows font file is redistributed.
The Options menu also supports importing a local font.

## Build

On Windows, install **JDK 21 x64** and **Python 3**, then run from the repo root:

```powershell
./desktop/Build-Windows.ps1 -Package
```

The script downloads two pinned, SHA-256-verified dependencies from Maven Central:
JNA 5.14.0 and JSON-java 20240303. It generates the shared client classes, compiles
the Windows adapter, and uses `jpackage` to include a reduced Java runtime.
Output: `desktop/build/dist/FNaF Redone/FNaF Redone.exe`.
Use `-OutputDirectory PATH` for a fresh packaging destination on subsequent builds.

Build the single-file installer with the Windows .NET Framework compiler (no
additional packaging tool required):

```powershell
./desktop/Build-Installer.ps1
./desktop/Test-Installer.ps1
```

Output: `desktop/build/installer/freddyfiles.exe` and `FreddyFiles-SHA256SUMS.txt`.
Use `-PayloadZip PATH` to embed an already-published portable ZIP unchanged.
Installer tests use isolated folders, shortcuts, and a temporary uninstall registry
key. They cover installation, locked files, rollback, downgrade refusal, the real
updater's folder swap, stable shortcuts, and uninstall after update. They retain
their test fixtures under the ignored build directory for inspection.

`generate.py` reads the Android source at build time. Simulation, progression,
rendering, assets, animation logic, and audio routing are reused. The host replaces
Android lifecycle, file picker, display management, and platform API implementations.
This keeps gameplay changes shared instead of maintaining a second game implementation.

## Validation

Verification across the Windows previews includes:

- All 31,642 shared JVM assertions.
- 35 desktop integration checks in 0.2.2, including real Steam EXE import (605 images,
  52 sounds), invalid-replacement recovery, keyboard/controller event routing,
  disconnect behavior, settings persistence, and HiDPI hit coordinates.
- Windows audio prepare, pause, resume, completion, and release checks.
- Visual inspection of title, office, cameras, and jumpscare rendering.
- Launch of the packaged EXE with its bundled runtime on the development PC.
- 11 library-discovery checks covering secondary and legacy libraries, custom
  installation folders, duplicate results, missing files, and malformed manifests.
- 31 display/update checks, plus scheduling tests for EDT confinement, bounded
  callback backlog, recovery, and shutdown. Live GitHub lookup was checked too.

The 0.2.2 camera-window check on the development PC measured **60.2 presented FPS**
at a 1904×1041 client size, with a 0.23 ms p95 event-queue delay. Five wall-clock
seconds advanced the simulation by five seconds. The previous timer path measured
about 32 FPS on that PC. These measurements are **not OXP3 benchmarks**.

Windows uses monotonic 60 Hz frame deadlines. Since 0.2.3, controller-driver queries
run on a separate thread and their ordered input is delivered before simulation.
Native mode is optional; the default Handheld mode avoids full-resolution scene
composition and the previous fallback's expensive filtered upscale.

In a software-rendering stress test at 2560×1440, the new Handheld camera path took
about **6.2 ms** per draw versus about **26 ms** for the old fallback on this PC.
This is a controlled rendering comparison, **not a confirmed OXP3 fix**. Reported
OXP3 performance remains unverified until the updated build is tested there.

Run `./desktop/Test-SteamLocator.ps1` for the asset-free scan tests. Add `-Installed`
to also check discovery against your actual Steam installation.
After building, run `./desktop/Test-Desktop.ps1` for scheduling, aspect-ratio, and
release-selection tests. Performance probes require a private imported Steam pack.

Controller tests inject XInput states into the input adapter; they do not certify
the OXP3’s physical buttons. Exact pixel/audio timing parity, full-night playthroughs,
handheld performance, native multi-touch, and other monitor configurations are not
fully verified. This is a **Windows preview**, not a claim of hardware certification.

Tests needing the owned Steam executable remain local; fixtures and extracted assets
are not included. See [third-party notices](THIRD-PARTY.md).
