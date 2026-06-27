# Windows service

The service uses WinSW v2.12.0.

## Files

- WinSW executable: `service/winsw/nfs-server.exe`
- WinSW configuration: `service/winsw/nfs-server.xml`
- Service id: `TinyWinNfsServer`
- Legacy service ids: `OgawaNfsServer`, `QnxNfsServer`
- Packaged Java runtime: `runtime/bin/java.exe`

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

- UDP `111` for portmap
- UDP `2049` for nfsd
- UDP `20048` for mountd

## Logs

WinSW writes service logs under `service/winsw`.

TinyWinNFS writes request diagnostics to:

```text
logs/nfs-server.log
```

The request log includes client address, XID, RPC program, version, procedure, status, and path where available.
Successful NFS READ request-level logs are suppressed by default to avoid excessive volume during normal file reads.
Set the Java system property below to include successful READ request logs:

```text
-Dtinywin.nfs.debug=true
```

## Smoke Test

After the service starts, run:

```powershell
.\scripts\smoke-service.ps1
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
