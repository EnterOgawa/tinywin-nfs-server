# Changelog

## 1.7.1 - Unreleased

- Moved installed runtime configuration, default export data, and server log paths to `ProgramData\EnterOgawa\TinyWinNFS Server`.
- Added one-time migration from legacy `Program Files` configuration into the new `ProgramData` data root.
- Resolved relative `export.path` values against the data root when the configuration file lives under `conf`.
- Updated the SWT manager service diagnostics to show application root, data root, config file, service executable, and log file separately.
- Updated installer, WinSW service configuration, service scripts, smoke tests, and documentation for the separated app/data layout.

## 1.7.0 - 2026-06-27

- Hardened NFSv2/NFSv3 `READLINK` so broken or unreadable symlinks return stable NFS statuses instead of escaping as transport-level handler errors.
- Added NFSv3 `READLINK` client allow-list enforcement to match other handle-based operations.
- Improved NFSv2/NFSv3 `SYMLINK` error mapping for Windows symlink privilege failures and invalid link targets.
- Added regression coverage for NFSv2/NFSv3 symlink creation, broken symlink `READLINK`, regular-file `READLINK`, and NFSv3 `MKNOD` `NOTSUPP`.
- Fixed installer packaging so the bundled Java runtime keeps `runtime\conf\security\java.security` and no longer fails with `Failed to launch JVM`.
- Improved installed manager shortcuts and configuration save handling so `Program Files` installs require Administrator launch with a clear message.
- Changed installed manager shortcuts back to the GUI executable and use Windows `RUNASADMIN` compatibility to avoid opening a command prompt.
- Documented link compatibility policy and Windows symlink limitations.

## 1.6.1 - 2026-06-27

- Added AUTH_SYS UID/GID based automatic permission identity responses with `permission.identity=auto`.
- Fixed Windows Client for NFS direct mounts so files created through NFS can be updated by the same anonymous client without per-IP profiles.
- Improved QNX 4.25 bulk copy throughput by avoiding export-wide link-count scans during NFS attribute responses.
- Added write file caching, asynchronous write defaults, UDP request worker dispatch, and lower-volume operational logging for large copy/delete workloads.
- Improved QNX `.nfsX*` delete compatibility for directory-style silly rename cleanup.
- Added regression coverage for AUTH_SYS attribute identity, QNX WRITE compatibility, recursive delete cleanup, and Windows Client for NFS UDP/TCP checks.

## 1.6.0 - 2026-06-27

- Added ONC RPC over TCP record marking support.
- Added TCP listeners for Portmap, MOUNT, and NFS using the configured service ports.
- Added Portmap TCP mappings for NFS v2/v3 and MOUNT v1-v3.
- Verified NFSv2/NFSv3 file operations over TCP using the same implementation as UDP.
- Extended Windows Client for NFS validation to cover both UDP and TCP transports.
- Updated firewall, service, installer, and documentation for TCP support.

## 1.5.0 - 2026-06-27

- Hardened NFSv3 weak cache consistency data so mutation replies return pre-operation attributes captured before file changes.
- Added NFSv3 COMMIT file sync handling and operation logging.
- Strengthened server-side export validation for missing, non-directory, unreadable, and unwritable paths.
- Made the SWT manager write configuration through a validated temporary file before replacing the existing configuration.
- Added regression coverage for missing export paths and NFSv3 WCC pre-operation sizes.
- Added a service stability script and v1.5.0 release checklist.
- Updated README and installer upgrade documentation for operational hardening.

## 1.4.0 - 2026-06-27

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
