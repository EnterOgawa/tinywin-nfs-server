# v1.6.0 release checklist

v1.6.0 is the TCP transport milestone for NFSv2/NFSv3 and MOUNT over ONC RPC.

## Scope

- ONC RPC over TCP record marking.
- TCP listeners for Portmap, MOUNT, and NFS using the configured service ports.
- Portmap TCP mappings for NFS v2/v3 and MOUNT v1-v3.
- NFSv2/NFSv3 file operations over TCP using the same implementation as UDP.
- Windows Client for NFS TCP validation.
- Firewall, service, installer, and documentation updates for TCP.

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

For an extended run:

```powershell
.\scripts\test-service-stability.ps1 -DurationMinutes 60 -IntervalSeconds 15 -RestartEveryIterations 10
```

## Windows Client for NFS verification

Run this on a machine where Windows Client for NFS is installed and UDP/TCP 111/2049/20048 are free:

```powershell
.\scripts\test-windows-nfs-client.ps1
.\scripts\test-windows-nfs-client.ps1 -Transport TCP
```

Run the two commands as independent checks. Windows Client for NFS can cache mount or portmap state when switching transports in quick succession.

Expected result for each transport:

```text
WINDOWS NFS CLIENT TEST PASSED
```

The TCP run must also confirm log entries from `server=nfs-mount-tcp` and `server=nfs-tcp`.
The default export name is unique per run to avoid Windows Client for NFS mount-handle cache reuse between UDP and TCP checks.

## Installer upgrade verification

1. Install the latest released package.
2. Configure at least one writable export.
3. Confirm the service is running and the UDP smoke test passes.
4. Install the v1.6.0 package over the existing installation.
5. Confirm the existing service is stopped, replaced, and started again.
6. Confirm `conf\nfs-server.properties` still contains the configured exports.
7. Run UDP smoke, TCP smoke, file-integrity smoke, and Windows Client for NFS checks.

## README verification

Before creating the release tag:

- Confirm README lists UDP/TCP as supported transports.
- Confirm unsupported items remain explicit: NLM/file locking, NFSv4, and NFSv3 `MKNOD`.
- Confirm install, upgrade, and verification commands match the package.
- Confirm QNX 4.25 remains documented as NFSv2/UDP validation.

## Release metadata

- All v1.6.0 milestone issues are closed.
- Release tag is `v1.6.0`.
- Release title is `TinyWinNFS Server v1.6.0`.
- Installer asset is `TinyWinNfsSetup.exe`.
- Include the installer SHA256 in the GitHub release notes.
