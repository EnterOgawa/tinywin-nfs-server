# Inno Setup installer and application icon

## Context

The app image folder contains multiple runtime and service files.
For distribution, a single Inno Setup installer is easier to handle.
The default Java application icon was also not suitable for the product.

## Changes

Added:

- `assets/qnx-nfs-server.ico`
- `assets/qnx-nfs-server.png`
- `scripts/generate-icon.ps1`
- `scripts/package-installer.ps1`
- `installer/qnx-nfs-server.iss`
- `docs/installer.md`

Updated:

- `scripts/package-manager.ps1`
- `scripts/install-service.ps1`
- `service/winsw/nfs-server.package.xml`
- `docs/manager_app.md`
- `docs/windows_service.md`

## Packaging Behavior

`scripts/package-manager.ps1` now:

- Generates the application icon when needed.
- Passes the icon to `jpackage`.
- Uses the packaged `runtime` for both the manager app and the WinSW service.
- Removes the old separate `service-runtime` folder.
- Copies only runtime operation scripts into the distribution folder.
- Removes service log files from the distribution folder.

The packaged service XML now uses:

```xml
<executable>%BASE%\..\..\runtime\bin\java.exe</executable>
```

## Installer Behavior

`scripts/package-installer.ps1` builds:

```text
dist\installer\QnxNfsServerSetup.exe
```

The Inno Setup installer:

- Installs the app image under `Program Files`.
- Creates Start Menu shortcuts.
- Optionally creates a desktop shortcut.
- Optionally installs or replaces the `QnxNfsServer` Windows service.
- Optionally adds Windows Firewall rules.
- Removes the service during uninstall.

## Validation

```text
scripts/compile.ps1
scripts/test.ps1
scripts/package-manager.ps1
scripts/package-installer.ps1
scripts/smoke-service.ps1
```

Result:

```text
TEST PASSED: 6 tests
SERVICE SMOKE TEST PASSED
Installer created: C:\develop\nfs\dist\installer\QnxNfsServerSetup.exe
Installer size: 38.71 MB
```
