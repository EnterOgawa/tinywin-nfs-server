# v1.7.1 release checklist

v1.7.1 is an installation layout hardening release that separates application files from mutable runtime data.

## Scope

- Store installed configuration under `C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf`.
- Store the default export folder under `C:\ProgramData\EnterOgawa\TinyWinNFS Server\export`.
- Store TinyWinNFS server logs under `C:\ProgramData\EnterOgawa\TinyWinNFS Server\logs`.
- Migrate a legacy `Program Files` configuration only when the new `ProgramData` configuration does not exist.
- Resolve relative `export.path` values against the data root when the configuration file is under `conf`.

## Local verification

```powershell
git diff --check
.\scripts\compile.ps1
.\scripts\test.ps1
.\scripts\package-installer.ps1
```

Expected unit-test result:

```text
TEST PASSED: 14 tests
```

## Installer verification

Install or upgrade with the service task enabled, then confirm:

- `C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf\nfs-server.properties` exists.
- `C:\ProgramData\EnterOgawa\TinyWinNFS Server\export` exists.
- `C:\ProgramData\EnterOgawa\TinyWinNFS Server\logs` exists.
- Existing configured exports are preserved during upgrade.
- The installed WinSW XML points the service to the `ProgramData` configuration file.
- The installed manager Service tab shows separate application root, data root, config file, service executable, and log file paths.

## Service verification

Run:

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

## Release metadata

- Release tag is `v1.7.1`.
- Release title is `TinyWinNFS Server v1.7.1`.
- Installer asset is `TinyWinNfsSetup.exe`.
- Include the installer SHA256 in the GitHub release notes.
