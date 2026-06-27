# Changelog

## 1.4.0 - Unreleased

- Added NFSv3 over UDP alongside the existing NFSv2 implementation.
- Added MOUNT v3 responses with opaque file handles and AUTH_NONE/AUTH_SYS flavors.
- Added portmap registrations for NFSv3 and MOUNT v3 over UDP.
- Added NFSv3 read-write, metadata, directory, filesystem info, and commit procedure coverage.
- Extended Windows Client for NFS verification to assert observed MOUNT v3 and NFSv3 RPCs.

## 1.3.0 - 2026-06-27

- Added per-export IPv4 client allow-lists for MOUNT and NFS requests.
- Hardened export validation for invalid names, unreadable folders, and writable exports pointing at non-writable folders.
- Improved manager service feedback with explicit Save + Restart results, administrator checks, service executable path, and config path display.
- Improved request diagnostics with client address, XID, program, version, procedure, status, and path where available.
- Suppressed successful NFS READ request-level logs unless debug logging is enabled.

## 1.2.0 - 2026-06-27

- Added a Windows Client for NFS integration test that mounts TinyWinNFS through the native Windows NFS client.
- Added a Windows-client test configuration profile for anonymous writable mounts.
- Documented Windows Client for NFS verification and release checklist steps.
- Included the Windows Client for NFS smoke test script in the packaged manager image.

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
