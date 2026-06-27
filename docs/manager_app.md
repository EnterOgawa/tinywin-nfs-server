# Manager app

`TinyWinNfsManager` is the SWT double-click entry point for installation and configuration.

## Build App Image

```powershell
.\scripts\package-manager.ps1
```

Output:

```text
dist\TinyWinNfsManager
```

## Build Installer

```powershell
.\scripts\package-installer.ps1
```

Output:

```text
dist\installer\TinyWinNfsSetup.exe
```

## Run

For normal status display and configuration editing:

```text
dist\TinyWinNfsManager\TinyWinNfsManager.exe
```

For service install, uninstall, start, stop, firewall changes, and privileged smoke testing:

```text
dist\TinyWinNfsManager\TinyWinNfsManager-Admin.cmd
```

## Distribution Layout

```text
TinyWinNfsManager/
  TinyWinNfsManager.exe
  TinyWinNfsManager-Admin.cmd
  runtime/
  app/tinywin-nfs-server.jar
  app/org.eclipse.swt.win32.win32.x86_64.jar
  assets/tinywin-nfs-server.png
  service/winsw/nfs-server.exe
  service/winsw/nfs-server.xml
  conf/nfs-server.properties
  export/
  scripts/
  docs/
```

The service executable is still WinSW. The manager app hides the command-line operations from users.

`runtime` is used by both the manager app and the WinSW service process.
The SWT jar is used only by the manager app.

If `TinyWinNfsServer`, `OgawaNfsServer`, or the legacy `QnxNfsServer` is already installed, the manager app uses the installed service path as the configuration root.
This prevents editing a copied app image while the Windows service is still reading another `conf/nfs-server.properties`.

## Share Tab

The Share tab manages server-side shared folders.

Each shared folder has:

- Export name, for example `/export`
- Windows folder path
- Writable flag

Use `Add`, `Apply`, and `Remove` to manage multiple shared folders.
The selected shared folder is used to generate the client mount command:

```text
mount_nfs <server-host>:<server-mount-name> <client-mount-point>
```

The server mount name is the NFS export name, for example `/export` or `/work`.
The client mount point is the local directory on the NFS client, for example `/mnt`.

## Options Tab

The Options tab manages advanced values:

- Display language
- Server host and client mount point for command generation
- Portmap, NFS, and MOUNT UDP ports
- UID/GID, file mode, directory mode, block size, and read size

## Language

The manager app supports English and Japanese UI resources.

The language is stored in `conf/nfs-server.properties`:

```text
ui.language=auto
```

Supported values are:

- `auto`: use the Windows/JVM locale
- `en`: English
- `ja`: Japanese

After changing the language, save the configuration and reopen the manager app.

## Export Folder

The client mount command does not use the Windows folder path directly.
It mounts the server export name:

```text
mount_nfs <server-host>:/export /mnt
```

After changing shared folders, use `Save + Restart` or restart the service from the `Service` tab.
The service reads `conf/nfs-server.properties` only when it starts.
