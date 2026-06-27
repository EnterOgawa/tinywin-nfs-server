# Inno Setup installer

## Build

```powershell
.\scripts\package-installer.ps1
```

Output:

```text
dist\installer\TinyWinNfsSetup.exe
```

## Installer Behavior

The installer contains the manager app, Java runtime, WinSW service wrapper, scripts, configuration, and docs.

Install tasks:

- Create a desktop shortcut.
- Stop and remove an existing `TinyWinNfsServer` Windows service before files are installed, then install it again.
- Stop and remove legacy `OgawaNfsServer` or `QnxNfsServer` services before installing `TinyWinNfsServer`.
- Add Windows Firewall rules for UDP `111`, `2049`, and `20048`.
- Start `TinyWinNfsServer` after installation when the service task is selected.

The installer requires administrator privileges because service installation and firewall changes are privileged operations.

## Upgrade Check

Before a release, verify upgrade behavior from the previous public installer:

1. Install the previous release.
2. Configure at least one writable export.
3. Confirm `TinyWinNfsServer` is running.
4. Run:

```powershell
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
```

5. Install the new package over the existing installation.
6. Confirm the service was stopped, replaced, and started again.
7. Confirm `conf\nfs-server.properties` still contains the configured exports.
8. Run:

```powershell
.\scripts\smoke-service.ps1
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
```

The upgrade must not leave stale `TinyWinNfsServer`, `OgawaNfsServer`, or `QnxNfsServer` services installed.

## Icon

The application and setup icon are generated from:

```text
assets\tinywin-nfs-server.ico
```

Regenerate the icon with:

```powershell
.\scripts\generate-icon.ps1
```
