# TinyWinNFS rename

## Summary

- Product name is now `TinyWinNFS Server`.
- Manager app name is now `TinyWinNfsManager`.
- Windows service id is now `TinyWinNfsServer`.
- Package jar name is now `tinywin-nfs-server.jar`.
- Installer output is now `TinyWinNfsSetup.exe`.

## Compatibility

- `OgawaNfsServer` and `QnxNfsServer` remain as legacy service ids for migration.
- `OGAWA_NFS_HOME` and `QNX_NFS_HOME` remain as legacy environment variables.

## Follow-up

- NFSv3, TCP transport, and multiple exports remain planned work.

## Validation

- `scripts\test.ps1`: passed.
- `scripts\install-service.ps1 -Force`: migrated service to `TinyWinNfsServer`.
- `scripts\smoke-service.ps1`: passed with `CREATE/WRITE/READ/REMOVE`.
- `dist\TinyWinNfsManager\scripts\smoke-service.ps1`: passed.
- `scripts\package-installer.ps1`: created `dist\installer\TinyWinNfsSetup.exe`.

## Window icon fix

- `TinyWinNfsManager` now loads `assets\tinywin-nfs-server.png` as the Swing window icon.
- `scripts\package-manager.ps1` now includes the `assets` directory in the app image.
- Rebuilt `dist\installer\TinyWinNfsSetup.exe` after the icon fix.

## Edit round-trip fix

- NFSv2 attributes now return `ctime` from the current modified time instead of Windows creation time.
- NFSv2 time values now preserve microsecond precision when possible.
- RPC logs now include the NFS/MOUNT result `status`.
- Unit tests now cover QNX-style truncate/write followed by Windows-side overwrite and NFS reread.
- Service smoke now covers `CREATE/WRITE/SETATTR/READ/REMOVE`.
