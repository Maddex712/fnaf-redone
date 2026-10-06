# Verified Steam import format

This reader intentionally supports one inspected Windows Steam FNaF 1 executable:

- File: `FiveNightsatFreddys.exe`
- Length: 231,442,963 bytes
- SHA-256: `862cd7ab7c81b20a4e848888bc2493dda0181759c39958a42cc5c3d02dfb195a`
- Fusion package signature: PAMU, offset 2,341,701, build 284.

The complete executable is hashed before parsing. A different build is rejected,
even if it has the same filename. Updating these mappings requires repeating the
full extraction comparison; do not disable the hash check.

The uncompressed image-bank container starts at 2,417,538 and ends at 151,589,230.
Its little-endian count is 605. Records contain a handle, inflated length,
compressed length, then a zlib payload. Handles in this build are one-based.
The inflated 32-byte image header contains data length at +8, width/height at
+12/+14, mode/flags at +16/+17, and transparent RGB at +28. All observed images
are mode 4; 520 use flag 0 and 85 use flag 16. Pixels are BGR with one extra pixel
per odd-width row. Flag 16 adds a separate alpha plane padded to four bytes per
row. Otherwise the transparent RGB is color-keyed. PNG output uses standard
RGBA scanlines and zlib through Java's standard library.

The sound-bank container starts at 151,591,803 and ends at 231,442,712. It contains
52 records: handle, checksum, references, inflated length, flags, frequency,
UTF-16 name length and compressed length, followed by zlib data. All flags are 1.
The name precedes an unchanged RIFF WAV payload; padded name characters are
excluded from the filename.

The implementation is purpose-written Java and contains no game event code,
asset payloads, extractor binaries or copied third-party extractor source.
FusionAssetLite was consulted for format research and used as an independent
local extraction oracle. It is not a dependency of the APK or its build.

`SteamImporterTest` compares decoded RGBA pixels for all 605 images and exact
bytes for all 52 WAVs, and checks rejection of an invalid executable before output
creation. Reference assets and the executable must remain private.

Android copies the selected document into a bounded temporary file, verifies it,
extracts into staging, and activates only a complete result. Failed imports leave
the previous game directory available. An interrupted directory swap is recovered
on the next launch. Temporary files are removed after ordinary completion/failure.
