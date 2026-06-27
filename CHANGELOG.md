# Changelog

## 1.1.0 - 2026-06-27

- Added NFSv2 `WRITECACHE` compatibility no-op.
- Improved `SETATTR` mode handling with Windows read-only mapping.
- Improved NFSv2 file attributes and `STATFS` value handling.
- Stabilized `READDIR` cookies across paged directory reads.
- Hardened Windows `LINK` and `SYMLINK` validation and error mapping.
- Added configurable filename charset support.

## 1.0.1 - 2026-06-27

- Added Apache License 2.0 for the project source.
- Added third-party license notices for packaged components.
- Included license files in the app image and installer.

## 1.0.0 - 2026-06-27

- Added Windows user-space NFSv2 server over UDP.
- Added MOUNT v1/v2 and AUTH_SYS support.
- Added read-write file operations for QNX 4.25 compatibility.
- Added multiple export configuration.
- Added SWT manager app with English/Japanese UI.
- Added WinSW Windows service integration.
- Added Inno Setup installer packaging.
