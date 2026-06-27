# License release update

## Summary
- Add Apache License 2.0 as the project license.
- Add third-party notices for SWT, WinSW, and the packaged Java runtime.
- Include license files in the packaged app image and installer.
- Bump installer metadata to 1.0.1.

## Changed files
- `LICENSE`: add Apache License 2.0 text with project copyright notice.
- `THIRD_PARTY_NOTICES.md`: add third-party component notices.
- `README.md:64`: add license section.
- `CHANGELOG.md:3`: add 1.0.1 release notes.
- `scripts/package-manager.ps1:16`: set the jpackage app version to 1.0.1.
- `scripts/package-manager.ps1:87`: copy `LICENSE` and `THIRD_PARTY_NOTICES.md` into the app image.
- `installer/tinywin-nfs-server.iss:2`: update `AppVersion` to 1.0.1.
- `installer/tinywin-nfs-server.iss:12`: add copyright metadata.
- `installer/tinywin-nfs-server.iss:20`: show the Apache license in the installer.
- `installer/tinywin-nfs-server.iss:21`: show third-party notices before installation.

## Validation
- `.\scripts\test.ps1`
  - Result: `TEST PASSED: 8 tests`
- `.\scripts\package-installer.ps1`
  - Result: `C:\develop\nfs\dist\installer\TinyWinNfsSetup.exe`
  - Confirmed that `LICENSE` and `THIRD_PARTY_NOTICES.md` are included in the app image and installer input.
