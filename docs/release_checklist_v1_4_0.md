# v1.4.0 release checklist

v1.4.0 is the NFSv3 / MOUNT v3 over UDP milestone.

## Source Checks

```powershell
git status -sb
.\scripts\compile.ps1
.\scripts\test.ps1
```

Expected unit test result:

```text
TEST PASSED: 12 tests
```

## Protocol Checks

- Portmap returns UDP ports for NFS v2 and v3.
- Portmap returns UDP ports for MOUNT v1, v2, and v3.
- MOUNT v1/v2 still returns the fixed 32-byte NFSv2 handle.
- MOUNT v3 returns an opaque file handle and AUTH_NONE/AUTH_SYS flavors.
- NFSv2 regression procedures still pass.
- NFSv3 procedures pass for GETATTR, SETATTR, LOOKUP, ACCESS, READ, WRITE, CREATE, MKDIR, REMOVE, RMDIR, RENAME, READDIRPLUS, FSSTAT, FSINFO, PATHCONF, and COMMIT.

## Windows Client for NFS

Run from an elevated PowerShell session:

```powershell
.\scripts\test-windows-nfs-client.ps1
```

Expected result:

```text
PASS: Windows Client for NFS mount
PASS: Windows Client for NFS v3 RPC
PASS: Windows Client for NFS create/read/update/rename/delete
PASS: Windows Client for NFS directory create/delete
PASS: Windows Client for NFS Japanese filename
WINDOWS NFS CLIENT TEST PASSED
```

The script validates that the server log contains both:

```text
program=100005 version=3
program=100003 version=3
```

## Out of Scope

- TCP transport
- NLM/file locking

## Installer

```powershell
.\scripts\package-installer.ps1
```

Verify the generated installer:

```powershell
(Get-Item .\dist\installer\TinyWinNfsSetup.exe).VersionInfo | Select-Object ProductVersion,ProductName,FileDescription
Get-FileHash .\dist\installer\TinyWinNfsSetup.exe -Algorithm SHA256
```

## GitHub

- All v1.4.0 milestone issues are closed.
- Release tag is `v1.4.0`.
- Release title is `TinyWinNFS Server v1.4.0`.
- Attach `dist\installer\TinyWinNfsSetup.exe`.
- Confirm the release asset SHA256 matches the local file.
