# Installation & setup

## What you need

- Android 8.0 or newer, with at least **1.5 GB free** during setup.
- Your own purchased **Windows Steam version of Five Nights at Freddy’s 1**.
- A way to copy one file from your PC to Android, such as USB.
- A controller is recommended; touch controls are also available.

This project does not use Steam account linking. You select a local game file.
The APK includes the Android runtime and custom presentation assets, but does not
include the original game's gameplay images or audio.

## First launch

1. Download the APK from [Releases](https://github.com/MjonirMade/fnaf-redone/releases/tag/v0.1.3-preview).
2. Install it on Android. It appears as **FNaF Redone**. Allow installation
   from your chosen browser/file manager if Android prompts you.
3. On your PC, open Steam → Library → Five Nights at Freddy’s → Manage →
   Browse local files.
4. Copy **FiveNightsatFreddys.exe** to Android’s **Downloads** folder. Copy the
   actual file, not a desktop shortcut. Do not send your Steam account details.
5. Open FNaF Redone → **New Game → Select Steam File**. In the Android
   file picker, open Downloads and select the executable.
6. Keep the app open until **Ready to play** appears, then start New Game.

Import takes place entirely on Android. No PC extractor, Python, .NET, Windows
emulator, Steam login, or network connection is required. After a successful
import, the extra EXE in Downloads can be deleted to reclaim space.
An existing `fnaf1-steam-284` data ZIP is also supported.

## Display settings

**Options → Screen mode** cycles through Auto, Single screen, and Dual screen.
Auto uses a second Android presentation display when available. Otherwise the
office and cameras share one full-screen viewport. A disconnected secondary
display falls back to single-screen mode.

- **Dual screen:** office on top, cameras below; a separate shift terminal in menus.
- **Single screen:** X or the touch monitor bar switches between office and cameras.
  Use **Options → Shift dashboard** for night selection, progress, wallet, and desk
  cosmetics. The shop, achievements, and challenges remain in the main menu.
- Use **Swap screens / Second display** if the display order is wrong.

## Font

Menus use Android’s built-in monospace font by default. You can import a font you
are entitled to use through **Options → Import local font**. The development
build used `consola.ttf` from Windows; it is not bundled here. Title and night-card
art imported from your game retains its original lettering.

## Updates, saves, and uninstalling

The public preview uses **dev.thor.fnaf.preview**, so it can coexist with the older
private FNaF Thor app. Each app has its own saves and imported data. This release
does not migrate progress from the private app.

If you already installed **FNaF Thor Preview (0.1.1)**, install FNaF Redone over it.
The public preview's package and signing identity are unchanged, so its saves and
imported data stay in place. Do not uninstall it before updating.

Install future compatible APKs over the preview to retain its local progress.
Importing replacement game data does not reset achievements or purchases.
**Uninstalling or clearing app data deletes local saves and imported assets.**
There is no cloud save or Steam save synchronization.

## Troubleshooting

| Problem | What to try |
| --- | --- |
| EXE missing in picker | Open Downloads/storage directly; Recent can hide executable files. |
| Unsupported game file | Compare your file with the [supported build](COMPATIBILITY.md). Renaming a different edition will not work. |
| Import runs out of space | Free at least 1.5 GB before setup. A failed replacement preserves previously installed game data. |
| Reversed screens | Options → Swap screens / Second display. |
| Unexpected controller buttons | Options → Controller check / Remap buttons. |
| App cannot update | Check the package and signing identity. Do not uninstall without considering save loss. |

Still stuck? [Report a bug](https://github.com/MjonirMade/fnaf-redone/issues/new?template=bug_report.yml)
with your device, Android version, app version, steps, and visible error.
Never attach the game EXE, extracted game assets, credentials, or personal data.
