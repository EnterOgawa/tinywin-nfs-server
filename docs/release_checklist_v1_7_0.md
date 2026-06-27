# v1.7.0 release checklist

v1.7.0 is a link compatibility release for QNX 4.25 and Windows Client for NFS scenarios.

## Scope

- NFSv2/NFSv3 `READLINK` stable status handling.
- NFSv3 `READLINK` client allow-list enforcement.
- NFSv2/NFSv3 `SYMLINK` Windows privilege and invalid-target error handling.
- Broken symlink `READLINK` regression coverage.
- NFSv3 `MKNOD` remains explicitly unsupported with `NOTSUPP`.

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

Expected result for each transport:

```text
WINDOWS NFS CLIENT TEST PASSED
```

## QNX verification

On QNX 4.25:

- Mount the export with NFSv2/UDP.
- Copy a directory tree that contains regular files, directories, and symlink entries.
- Confirm regular files and directories are copied correctly.
- Confirm symlink entries either become real Windows symlinks or fail without creating corrupt placeholder files.
- Delete the copied tree from QNX.
- Confirm the Windows export no longer contains copied content or `.nfsX*` leftovers.
- Confirm `logs/nfs-server.log` has no current parse-error or unexpected mutation failures.

## README verification

Before creating the release tag:

- Confirm README states v1.7.0 support scope.
- Confirm link compatibility limitations are explicit.
- Confirm unsupported items remain explicit: NFSv3 `MKNOD`, NLM/file locking, and NFSv4.
- Confirm QNX 4.25 remains documented as NFSv2/UDP validation.
- Confirm Windows Client for NFS remains documented as NFSv3 UDP/TCP validation.
- Confirm the installed runtime contains `runtime\conf\security\java.security`.
- Confirm `TinyWinNfsManager.exe` starts from the installed folder without `Failed to launch JVM`.
- Confirm the installed Start Menu/Desktop manager shortcuts target `TinyWinNfsManager.exe`.
- Confirm the installed manager executable has the `RUNASADMIN` compatibility registry value.
- Confirm saving configuration from a non-Administrator direct launch shows the Administrator-required message instead of a temporary file path.

## Release metadata

- Release tag is `v1.7.0`.
- Release title is `TinyWinNFS Server v1.7.0`.
- Installer asset is `TinyWinNfsSetup.exe`.
- Include the installer SHA256 in the GitHub release notes.
