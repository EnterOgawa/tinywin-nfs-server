# v1.5.0 release checklist

v1.5.0 is the operational stability milestone after the NFSv3 release.

## Scope

- Write-path consistency for overwrite, truncate, rename, and COMMIT.
- Attribute and cache consistency after cross-client edits.
- Long-running service stability verification.
- RPC error handling and operational log readability.
- Multi-export configuration validation and safe persistence.
- Installer upgrade, service replacement, and settings preservation.
- README review before tagging.

## Local verification

```powershell
git diff --check
.\scripts\compile.ps1
.\scripts\test.ps1
.\scripts\package-installer.ps1
```

## Windows Client for NFS verification

Run this on a machine where Windows Client for NFS is installed and UDP 111/2049/20048 are free:

```powershell
.\scripts\test-windows-nfs-client.ps1
```

Expected result:

```text
WINDOWS NFS CLIENT TEST PASSED
```

## Service verification

Install or upgrade the service, then run:

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
.\scripts\smoke-service.ps1 -RestartHandlePersistence
```

For an extended run:

```powershell
.\scripts\test-service-stability.ps1 -DurationMinutes 60 -IntervalSeconds 15 -RestartEveryIterations 10
```

## Installer upgrade verification

1. Install the latest released package.
2. Configure at least one writable export.
3. Confirm the service is running and the smoke test passes.
4. Install the v1.5.0 package over the existing installation.
5. Confirm the existing service is stopped, replaced, and started again.
6. Confirm `conf\nfs-server.properties` still contains the configured exports.
7. Run the service and Windows Client for NFS checks again.

## README verification

Before creating the release tag:

- Confirm README lists the current supported protocols and tested clients.
- Confirm unsupported items remain explicit: NLM/file locking, TCP transport, and NFSv4.
- Confirm install, upgrade, and verification commands match the package.
- Confirm multi-export and writable-share guidance matches the manager UI.

## Release metadata

- All v1.5.0 milestone issues are closed.
- Release tag is `v1.5.0`.
- Release title is `TinyWinNFS Server v1.5.0`.
- Installer asset is `TinyWinNfsSetup.exe`.
- Include the installer SHA256 in the GitHub release notes.
