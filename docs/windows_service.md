# Windows service

The service uses WinSW v2.12.0.

## Files

- WinSW executable: `service/winsw/nfs-server.exe`
- WinSW configuration: `service/winsw/nfs-server.xml`
- Service id: `TinyWinNfsServer`
- Legacy service ids: `OgawaNfsServer`, `QnxNfsServer`
- Packaged Java runtime: `runtime/bin/java.exe`
- Data root: `C:\ProgramData\EnterOgawa\TinyWinNFS Server`

The packaged manager and service share the same Java runtime.

## Install

Run PowerShell as Administrator.

```powershell
.\scripts\download-winsw.ps1
.\scripts\install-service.ps1
.\scripts\add-firewall-rules.ps1
```

The Inno Setup installer can also run service installation and firewall setup as install tasks.

## Start

```powershell
.\scripts\start-service.ps1
.\scripts\status-service.ps1
```

## Stop

```powershell
.\scripts\stop-service.ps1
```

## Uninstall

```powershell
.\scripts\uninstall-service.ps1
```

## Ports

Default configuration uses:

- UDP/TCP `111` for portmap
- UDP/TCP `2049` for nfsd
- UDP/TCP `20048` for mountd

## Logs

WinSW writes service logs under `service/winsw`.

TinyWinNFS writes request diagnostics to:

```text
C:\ProgramData\EnterOgawa\TinyWinNFS Server\logs\nfs-server.log
```

By default, TinyWinNFS keeps operational logs useful for troubleshooting while suppressing high-volume success logs such as RPC request traces and successful mutation operations. This avoids excessive synchronous I/O during large file copies and bulk deletes.

Write responses use `write.sync=false` by default so large copies can use the Windows file cache. Set `write.sync=true` in `C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf\nfs-server.properties` only when each WRITE must be physically synchronized before the server replies.

Write file caching is also enabled by default. It keeps recently written files open for a short time to avoid repeated open/close overhead:

```text
write.cache.enabled=true
write.cache.max.open=64
write.cache.idle.millis=3000
```

The server closes cached files before `SETATTR`, `REMOVE`, `RMDIR`, `RENAME`, and service shutdown so normal file operations are not left blocked by the cache.

UDP RPC requests are processed by a worker pool so request handling is not tied to the receive loop. The default worker count is based on CPU count and capped at 8. Set the Java system property below to override it:

```text
-Dtinywin.nfs.udp.workers=4
```

On Windows, NFS attribute `nlink` uses the platform value when available. When Windows cannot expose a native link count, regular files report a lightweight count, with NFS-created hard links tracked in memory. This avoids scanning the entire export tree for every attribute response during large QNX copies.

Set the Java system property below to include RPC request-level logs:

```text
-Dtinywin.nfs.requestLog=true
```

Set the Java system property below to include all debug and high-volume operation logs:

```text
-Dtinywin.nfs.debug=true
```

File handle persistence is saved in 30-second batches by default during runtime and flushed when the service stops. Set the interval to `0` for immediate persistence after every handle change:

```text
-Dtinywin.nfs.handleStoreSaveIntervalMillis=0
```

## Permission Identity

The default permission identity mode is:

```text
permission.identity=auto
```

In auto mode, TinyWinNFS returns the AUTH_SYS UID/GID sent by the current client in NFS file attributes.
This avoids per-client IP profiles and lets QNX, Windows Client for NFS, and other AUTH_SYS clients use their own identity semantics.

Set the value below only when attributes must always use the configured `uid` and `gid`:

```text
permission.identity=fixed
```

## Smoke Test

After the service starts, run:

```powershell
.\scripts\smoke-service.ps1
```

For TCP transport, run:

```powershell
.\scripts\smoke-service.ps1 -Transport TCP
```

Expected result:

```text
PASS: service portmap GETPORT
PASS: service mount MNT
PASS: service nfs GETATTR
PASS: service nfs CREATE/WRITE/SETATTR/READ/RENAME/REMOVE
SERVICE SMOKE TEST PASSED
```

To verify that file handles remain usable across a service restart, run:

```powershell
.\scripts\smoke-service.ps1 -RestartHandlePersistence
```

To verify that NFS writes do not corrupt the exported Windows file content, run:

```powershell
.\scripts\smoke-service.ps1 -VerifyFileIntegrity
```
