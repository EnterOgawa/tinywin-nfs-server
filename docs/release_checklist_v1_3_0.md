# v1.3.0 release checklist

v1.3.0 is the operational hardening milestone.

## Source Checks

```powershell
git status -sb
.\scripts\compile.ps1
.\scripts\test.ps1
```

Expected unit test result:

```text
TEST PASSED: 11 tests
```

## Functional Checks

- Invalid export names are rejected before save/start.
- Missing, unreadable, or non-writable folders are rejected when applicable.
- `allowed.clients` empty means all clients are allowed.
- `allowed.clients=127.0.0.1` allows local tests and rejects another IPv4 address.
- Save + Restart logs save and restart results separately.
- The Service tab shows application root, config file, and service executable path.
- Service operations show an Administrator privilege error when not elevated.

## Log Checks

Confirm request diagnostics include:

- client address
- XID
- RPC program/version/procedure
- status
- path for mutation or access-denied operations

Successful NFS READ request logs should be suppressed unless `-Dtinywin.nfs.debug=true` is set.

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

- All v1.3.0 milestone issues are closed.
- Release tag is `v1.3.0`.
- Release title is `TinyWinNFS Server 1.3.0`.
- Attach `dist\installer\TinyWinNfsSetup.exe`.
- Confirm the release asset SHA256 matches the local file.
