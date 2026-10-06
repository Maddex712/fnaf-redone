# Windows distribution notices

Project-authored Windows adapter code is covered by the repository's MIT code
license. Game and art rights remain as described in [CREDITS](../CREDITS.md).

- **Java Native Access 5.14.0**: https://github.com/java-native-access/jna/tree/5.14.0
  Distributed under its Apache License 2.0 option. See `JNA-LICENSE.txt` and
  `APACHE-2.0.txt`. JNA's native libraries remain inside its unmodified JAR.
- **JSON-java 20240303**: https://github.com/stleary/JSON-java/tree/20240303
  Public domain. See `JSON-LICENSE.txt`.
- **Bundled Java runtime**: the published Windows preview uses Microsoft OpenJDK
  21.0.12.1+1-LTS for x86_64, reduced with `jlink` by `jpackage`. Runtime license
  texts and notices are preserved under `runtime/legal/` in the download.
  Corresponding source revision:
  https://github.com/microsoft/openjdk-jdk21u/tree/6f26c6b2b098814858e76ad2986c01d5e1c8987a
  Source download:
  https://github.com/microsoft/openjdk-jdk21u/archive/6f26c6b2b098814858e76ad2986c01d5e1c8987a.zip
  Rebuilding with another JDK vendor/version requires retaining that runtime's
  license notices and updating this distribution record accordingly.

XInput layout reference:
https://learn.microsoft.com/en-us/windows/win32/api/xinput/ns-xinput-xinput_gamepad

No Steam executable, extracted game images/audio, user saves, or Windows font files
are included in the portable application. Earlier project assets and AI assistance
are documented in the repository credits.
