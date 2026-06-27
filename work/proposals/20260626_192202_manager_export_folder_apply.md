# Manager export folder apply workflow

## Context

QNX 4.25 can mount the current `/export` successfully after starting `NFSfsys`.
The next target is to mount the Windows folder selected by the manager app.

## Change Summary

Updated the manager app so that changing the Windows export folder can be applied from the GUI.

Changed:

- `src/jp/co/enterogawa/nfs/manager/QnxNfsServerManager.java`
- `scripts/restart-service.ps1`
- `docs/manager_app.md`
- `docs/qnx425_mount.md`

## Behavior

The QNX command still mounts the server export name:

```sh
mount_nfs <server-host>:/export /mnt
```

The Windows folder behind `/export` is selected in the manager app:

```text
Settings -> Export path -> Browse
```

After changing the folder, use:

```text
Save + Restart
```

This saves `conf/nfs-server.properties` and restarts the Windows service so the new `export.path` is loaded.

## Validation

```text
scripts/compile.ps1
scripts/test.ps1
scripts/package-manager.ps1
```

Result:

```text
TEST PASSED: 6 tests
App image created: C:\develop\nfs\dist\QnxNfsServerManager
```

The service restart was not executed automatically to avoid interrupting the QNX-mounted session.
