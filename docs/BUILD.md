# Build from source

Run commands from the repository root. Windows PowerShell is the supported APK build environment.


Requires JDK 21, Android platform 35 and Android build-tools 35. Arrange the Android
platform under `TOOLCHAIN/platform/` and build tools under `TOOLCHAIN/build/`.
Their standard subfolders may be nested; the build script discovers the tools.
Supply your own private signing key and set `FNAF_SIGNING_PASSWORD` in the build
environment. Then run:

```powershell
./Build.ps1 -Toolchain PATH_TO_TOOLCHAIN -SigningKey PATH_TO_PRIVATE_KEY -KeyAlias thor
```

The script runs the simulation, progress, title, intro and outro tests and signs
the APK. It never generates or includes a publisher key. Different signing keys
cannot update this publisher's existing installation.

To verify the direct importer with your own reference extraction:

```powershell
javac --release 8 -d build/import-tests app/src/main/java/dev/thor/fnaf/SteamImporter.java tests/SteamImporterTest.java
java -Xmx256m -cp build/import-tests dev.thor.fnaf.SteamImporterTest PATH_TO_EXE PATH_TO_PRIVATE_REFERENCE PATH_TO_NEW_OUTPUT
```


## Run the asset-free tests

```powershell
./Test.ps1
./Test-AndroidUpdates.ps1
```

These tests need JDK 21 but no Android SDK, game installation, or signing key.
The GitHub Actions workflow runs the same six suites plus the Android update checks.
Update tests download a SHA-256-pinned JSON library for JVM testing; Android itself
uses its built-in JSON API. Importer comparison tests
are deliberately separate because they need a privately owned game executable
and reference extraction; do not upload either to CI or issues.

## Android SDK folder layout

```text
TOOLCHAIN/
  platform/android-35/android.jar
  build/android-15/aapt2.exe
  build/android-15/d8.bat
  build/android-15/zipalign.exe
  build/android-15/apksigner.bat
```

The inner build-tools directory name is not significant. Use the complete
Android build-tools 35 distribution, not just the files listed above.
`javac`, `java`, and `jar` must be available from your JDK installation.

The source builds package `dev.thor.fnaf.preview`, version `0.1.5-preview`.
Supply your own signing key; publisher keys and passwords are never checked in.
GitHub CI tests source only and does not publish or sign APKs.

Android update verification also has a separate instrumentation fixture in
`tests/android/`. After building the APK, `Build-UpdateProbe.ps1` (with the same
toolchain/signing arguments and signing environment variable) builds an older-version
fixture and a test-only instrumentation APK under `build/android-update-device`.
Use these only on an isolated emulator. They are never part of the release APK.
The `prepare` mode validates a supplied newer APK and tests the restricted provider;
`verify` checks the installed version, test preferences/private data, and cache cleanup
after confirming the update in Android's actual installer. `network` checks GitHub
availability separately. An unavailable GitHub repository prevents end-to-end live
download verification, even when local verification and Android installation pass.
