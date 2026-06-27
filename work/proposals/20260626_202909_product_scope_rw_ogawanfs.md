# OgawaNFS product scope and read-write update

## Summary

- Product direction changed from QNX-only naming to a general Windows NFS server.
- Tentative product name is `OgawaNFS Server`.
- Existing `QnxNfsServer` Windows service was migrated to `OgawaNfsServer`.
- NFSv2 write-side MVP was implemented and verified through unit tests and UDP service smoke test.

## Name Research

Web/GitHub searches were checked on 2026-06-26.

- `haneWIN NFS Server` already exists and supports NFS 2/3 on Windows.
- `WinNFSd` already exists and is a Windows NFSv3 server with read/write support.
- `EnterNFS` / `Enternfs` has search noise and should not be used.
- `OgawaNFS` and `OgawaNFS Server` had no exact product/GitHub hit in the performed searches, so this is the current candidate.

Sources:

- https://www.hanewin.net/nfs-e.htm
- https://www.hanewin.net/doc/nfs/nfsd.htm
- https://github.com/winnfsd/winnfsd
- https://sourceforge.net/projects/winnfsd/
- https://datatracker.ietf.org/doc/html/rfc1094
- https://datatracker.ietf.org/doc/html/rfc1813

## Implemented Now

- Added `export.writable=true` configuration.
- Changed default returned modes to `0644` for files and `0755` for directories.
- Implemented NFSv2 write-side procedures:
  - `SETATTR`
  - `WRITE`
  - `CREATE`
  - `REMOVE`
  - `RENAME`
  - `LINK`
  - `SYMLINK`
  - `MKDIR`
  - `RMDIR`
- Updated `FileHandleTable` so rename/delete operations update cached handles.
- Added read-write unit tests.
- Added service smoke test coverage for `CREATE/WRITE/READ/REMOVE`.
- Added `Writable` checkbox to the manager.
- Renamed packaged app image to `OgawaNfsManager`.
- Renamed installer output to `OgawaNfsSetup.exe`.
- Added `java.exe` into packaged runtime because the jpackage runtime image does not include the launcher by default.

## Current Verified Outputs

- App image:
  - `dist\OgawaNfsManager\OgawaNfsManager.exe`
- Installer:
  - `dist\installer\OgawaNfsSetup.exe`
- Windows service:
  - `OgawaNfsServer`

## Verification

- `scripts\test.ps1`
  - `TEST PASSED: 6 tests`
- `scripts\smoke-service.ps1`
  - `PASS: service portmap GETPORT`
  - `PASS: service mount MNT`
  - `PASS: service nfs GETATTR`
  - `PASS: service nfs CREATE/WRITE/READ/REMOVE`
- `dist\OgawaNfsManager\scripts\smoke-service.ps1`
  - same service read-write smoke result
- `scripts\package-installer.ps1`
  - created `dist\installer\OgawaNfsSetup.exe`

## Remaining Full Product Scope

NFSv2 read-write is now an MVP, not the final full implementation.

Next implementation phases:

1. Multiple export/mount points.
   - Replace single `export.name/export.path` with an export list.
   - Update MOUNT export list and MNT resolution.
   - Add manager table UI for add/remove/edit exports.
2. NFSv3.
   - Add program version 3 for NFS.
   - Add MOUNT v3.
   - Implement 64-bit file sizes, `READDIRPLUS`, `FSINFO`, `PATHCONF`, `WRITE` stability, and `COMMIT`.
3. TCP transport.
   - Add record-marking RPC over TCP.
   - Register UDP and TCP mappings in portmap.
4. Compatibility hardening.
   - QNX regression.
   - WSL/Linux read-write mount.
   - Filename encoding behavior.
   - Windows file sharing/locking behavior.
   - NLM decision and documentation.
