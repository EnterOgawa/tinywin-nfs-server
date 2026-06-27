# v1.2.0 release checklist

v1.2.0 is the Windows Client for NFS integration milestone.

## Source Checks

```powershell
git status -sb
.\scripts\compile.ps1
.\scripts\test.ps1
```

Expected unit test result:

```text
TEST PASSED: 9 tests
```

## Windows Client for NFS Check

Verify prerequisites:

```powershell
Get-Command mount.exe
Get-Service NfsClnt
```

Run the mount smoke test:

```powershell
.\scripts\test-windows-nfs-client.ps1
```

Expected result:

```text
WINDOWS NFS CLIENT TEST PASSED
```

The script must leave no mounted test drive and no temporary TinyWinNFS server process.

## Optional Client Checks

QNX 4.25 on VMware remains the legacy-client compatibility check.

Linux VM mount tests may be used as additional verification.

WSL is optional and is not a v1.2.0 release gate.

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

- All v1.2.0 milestone issues are closed.
- Release tag is `v1.2.0`.
- Release title is `TinyWinNFS Server 1.2.0`.
- Attach `dist\installer\TinyWinNfsSetup.exe`.
- Confirm the release asset SHA256 matches the local file.
