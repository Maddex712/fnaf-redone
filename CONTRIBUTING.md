# Contributing

Bug reports, device compatibility results, and focused fixes are welcome.
This is an early public preview; there is no promised support response time.

## Report a problem

Use the bug report template. Include the release version, device, Android version,
screen mode, controller, reproduction steps, and expected versus actual behavior.
For gameplay differences, name the night and provide a short comparison with your
own original game when possible. Screenshots and short clips help with layout bugs.

Do not upload Steam executables, extracted game assets, saves containing personal
information, credentials, or signing keys. Redact personal details from logs.

## Submit a change

1. Open an issue for substantial design or gameplay changes before implementing them.
2. Work on a focused branch and describe the problem your change solves.
3. Run `./Test.ps1` with JDK 21. For Android changes, build and test the affected flow.
4. Include screenshots for UI changes and device details for hardware-specific fixes.
5. State what you tested and any remaining limitations in the pull request.

Keep gameplay difficulty separate from cosmetic systems. Do not loosen importer
validation without supporting the new file format and adding comparison evidence.
Keep game files and private signing material outside the repository. Code and docs
follow the MIT license; only contribute art you can license, with explicit credits
and terms. Disclose generated or AI-assisted art so its provenance remains clear.

See [Build from source](docs/BUILD.md) and [Credits](CREDITS.md).
