# Compatibility

## Devices

| Platform | Current status |
| --- | --- |
| AYN Thor | Tested hardware; public preview installed and imported successfully. Both menu screens visually checked. |
| Android 35 emulator | Direct EXE import, Night 1 startup, single-screen routing, and virtual dual-display tests passed. |
| BlueStacks | Steam EXE import completed successfully in a manual check. |
| Other Android phones/tablets | Single-screen support included; physical-device testing still needed. |
| Other dual-screen Android devices | Requires Android presentation-display support; not certified. |
| iOS / Windows desktop | No application build provided. Windows is used only to obtain your Steam file and build source. |

Android **8.0 / API 26** is the minimum. Allow at least **1.5 GB free** during
import. The app targets Android API 35. A controller is recommended, with touch
controls also available. Artwork is fitted to a 16:9 viewport rather than stretched.

## Supported game file

Only this inspected Windows Steam FNaF 1 build is accepted:

| Field | Value |
| --- | --- |
| Filename | `FiveNightsatFreddys.exe` |
| Size | 231,442,963 bytes |
| SHA-256 | `862cd7ab7c81b20a4e848888bc2493dda0181759c39958a42cc5c3d02dfb195a` |

To check your file on Windows:

```powershell
Get-FileHash 'PATH_TO_GAME/FiveNightsatFreddys.exe' -Algorithm SHA256
```

Other storefronts, mobile editions, modified executables, and other Steam builds
are not supported by this importer. An existing prepared `fnaf1-steam-284` ZIP
is accepted. Do not bypass the hash check: asset mappings are specific to this build.

## What “preview” means

The Android runtime reconstructs the original game’s behavior. The aim is faithful
gameplay and difficulty; this release does not certify frame-perfect animation,
AI, timing, or every night against the original executable. Custom menu and death
vignettes are fan presentation, not claims about official canon.

Automated tests cover game state, progression, title effects, intro/outro, and screen
modes. Separate local checks compared 605 imported images and 52 sounds with the
reference extraction. These checks cannot substitute for full playthroughs on all
devices. See [the verification record](../VERIFICATION.md) for details.

Progress is local to the application. There is no cloud save, Steam account
connection, or synchronization with the original PC game.
