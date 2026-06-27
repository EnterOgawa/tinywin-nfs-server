# Manager and installed service root mismatch

## Context

The manager app displayed:

```text
Export path = C:\develop\nfs\qnxwork
```

But QNX still mounted the old folder:

```text
C:\develop\nfs\export
```

## Cause

Two different configuration files existed.

The manager app launched from:

```text
C:\develop\nfs\dist\QnxNfsServerManager
```

and edited:

```text
C:\develop\nfs\dist\QnxNfsServerManager\conf\nfs-server.properties
```

The installed Windows service was registered to:

```text
C:\develop\nfs\service\winsw\nfs-server.exe
```

and read:

```text
C:\develop\nfs\conf\nfs-server.properties
```

Therefore, the GUI value did not affect the running service.

## Changes

Updated:

- `src/jp/co/enterogawa/nfs/manager/QnxNfsServerManager.java`
- `conf/nfs-server.properties`
- `docs/manager_app.md`

The manager now checks the installed `QnxNfsServer` service path first.
When the service is already installed, the manager uses the installed service root as its configuration root instead of blindly using the app image location.

The active service configuration was updated to:

```properties
export.path=C:\\develop\\nfs\\qnxwork
```

## Verification

```text
scripts/compile.ps1
scripts/test.ps1
scripts/restart-service.ps1
scripts/smoke-service.ps1
```

Result:

```text
TEST PASSED: 6 tests
SERVICE SMOKE TEST PASSED
QnxNfsServer Running
```

## Remaining Packaging Note

`scripts/package-manager.ps1` could not regenerate `dist\QnxNfsServerManager` because running `QnxNfsServerManager.exe` processes were holding `app\qnx-nfs-server.jar`.
Close the manager windows before regenerating the app image.
