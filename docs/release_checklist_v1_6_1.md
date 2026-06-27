# v1.6.1 release checklist

v1.6.1 is a compatibility and performance stabilization release after the TCP transport milestone.

## Scope

- AUTH_SYS UID/GID based automatic permission identity with `permission.identity=auto`.
- Windows Client for NFS direct mount compatibility without client-IP-specific profiles.
- QNX 4.25 bulk copy throughput improvements.
- QNX `.nfsX*` directory cleanup compatibility.
- Write file cache and async write default behavior.
- Reduced high-volume success logging during large copy and delete workloads.

## Local verification

```powershell
git diff --check
.\scripts\compile.ps1
.\scripts\test.ps1
.\scripts\package-installer.ps1
```

Expected unit-test result:

```text
TEST PASSED: 13 tests
```

## Service verification

Install or upgrade the service, then run:

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -Transport TCP
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
.\scripts\smoke-service.ps1 -RestartHandlePersistence
```

Expected result:

```text
SERVICE SMOKE TEST PASSED
PASS: service file integrity
PASS: service handle persistence after restart
```

## Windows Client for NFS verification

Run the Windows Client for NFS integration checks:

```powershell
.\scripts\test-windows-nfs-client.ps1
.\scripts\test-windows-nfs-client.ps1 -Transport TCP
```

Run the two commands as independent checks. Windows Client for NFS can cache mount or portmap state when switching transports in quick succession.

Expected result for each transport:

```text
WINDOWS NFS CLIENT TEST PASSED
```

Also verify the installed service export directly when `permission.identity=auto` is enabled:

- Mount `\\127.0.0.1\export` with Windows Client for NFS.
- Create a file.
- Append to the same file.
- Rename the file.
- Delete the file and its parent test directory.
- Confirm no temporary mount drive remains.

## QNX verification

On QNX 4.25:

- Mount the export with NFSv2/UDP.
- Copy a large directory tree from QNX to the Windows export.
- Confirm the copy completes without `.nfsX*` leftovers.
- Delete the copied tree from QNX.
- Confirm the Windows export no longer contains the copied tree.
- Confirm `logs/nfs-server.log` has no current `status=13`, parse-error, or unexpected mutation failures.

## Installer upgrade verification

1. Install the latest released package.
2. Configure at least one writable export.
3. Confirm the service is running and the UDP smoke test passes.
4. Install the v1.6.1 package over the existing installation.
5. Confirm the existing service is stopped, replaced, and started again.
6. Confirm `conf\nfs-server.properties` still contains the configured exports.
7. Confirm `permission.identity=auto` is present or add it through the manager.
8. Run UDP smoke, TCP smoke, file-integrity smoke, and Windows Client for NFS checks.

## README verification

Before creating the release tag:

- Confirm README states v1.6.1 support scope.
- Confirm `permission.identity=auto/fixed` is documented.
- Confirm unsupported items remain explicit: NFSv3 `MKNOD`, NLM/file locking, and NFSv4.
- Confirm QNX 4.25 remains documented as NFSv2/UDP validation.
- Confirm Windows Client for NFS remains documented as NFSv3 UDP/TCP validation.

## Release metadata

- Release tag is `v1.6.1`.
- Release title is `TinyWinNFS Server v1.6.1`.
- Installer asset is `TinyWinNfsSetup.exe`.
- Include the installer SHA256 in the GitHub release notes.
