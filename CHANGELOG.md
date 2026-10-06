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
