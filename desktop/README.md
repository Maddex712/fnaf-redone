# FNaF Redone for Windows

A portable **Windows x64** client for handhelds such as the OXP3 and for desktop PCs.
This is the same gameplay and menu code used by the Android port, hosted by a
Windows Java2D/audio/input adapter. It does not run Android or emulate the APK.

## Play

1. Download the [Windows preview ZIP](https://github.com/MjonirMade/fnaf-redone/releases/tag/v0.2.1-windows-preview).
2. Extract the **entire** ZIP into a folder. Keep `app` and `runtime` beside the EXE.
3. Run **FNaF Redone.exe**. No separate Java installation is needed.
4. Choose **New Game → Scan Steam** (A / Enter). The client finds your installed
   FNaF 1 copy and imports it. Use **Browse File** (X / Space) to select an EXE or
   existing data ZIP yourself.
5. Wait for **Ready to play** and start the game.

You need Windows 10/11 **64-bit**, your purchased Windows Steam FNaF 1 copy, and
at least **1.5 GB free** for import. The supported game file is the same as the
[Android build](../docs/COMPATIBILITY.md#supported-game-file). This preview is not
code-signed. Verify its SHA-256 against the release checksum file.

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

`generate.py` reads the Android source at build time. Simulation, progression,
rendering, assets, animation logic, and audio routing are reused. The host replaces
Android lifecycle, file picker, display management, and platform API implementations.
This keeps gameplay changes shared instead of maintaining a second game implementation.

## Validation

The initial Windows preview passed:

- All 31,642 shared JVM assertions.
- 27 desktop integration checks, including setup-button scan and automatic Steam EXE import (605 images,
  52 sounds), invalid-replacement recovery, keyboard/controller event routing,
  disconnect behavior, settings persistence, and HiDPI hit coordinates.
- Windows audio prepare, pause, resume, completion, and release checks.
- Visual inspection of title, office, cameras, and jumpscare rendering.
- Launch of the packaged EXE with its bundled runtime on the development PC.
- 11 library-discovery checks covering secondary and legacy libraries, custom
  installation folders, duplicate results, missing files, and malformed manifests.

Run `./desktop/Test-SteamLocator.ps1` for the asset-free scan tests. Add `-Installed`
to also check discovery against your actual Steam installation.

Controller tests inject XInput states into the input adapter; they do not certify
the OXP3’s physical buttons. Exact pixel/audio timing parity, full-night playthroughs,
handheld performance, native multi-touch, and other monitor configurations are not
fully verified. This is a **Windows preview**, not a claim of hardware certification.

Tests needing the owned Steam executable remain local; fixtures and extracted assets
are not included. See [third-party notices](THIRD-PARTY.md).
