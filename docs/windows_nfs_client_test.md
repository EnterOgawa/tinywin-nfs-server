# Windows Client for NFS mount test

Windows Client for NFS is the primary local integration test for v1.2.0. It verifies that TinyWinNFS can be mounted by a native Windows NFS client without WSL or Hyper-V.

QNX 4.25 on VMware remains the legacy-client compatibility reference. WSL checks are optional only.

## Prerequisites

- Windows Client for NFS is enabled.
- The `NfsClnt` service is running.
- PowerShell is running as Administrator.
- UDP ports `111`, `2049`, and `20048` are free.
- Drive `Z:` is free, or another free drive letter is passed to the script.

Check the client:

```powershell
Get-Command mount.exe
Get-Service NfsClnt
```

## Automated Test

Run:

```powershell
.\scripts\test-windows-nfs-client.ps1
```

The script:

- creates a temporary export under `work\tmp`;
- starts TinyWinNFS with a Windows-client test configuration;
- mounts `\\127.0.0.1\export` to `Z:`;
- verifies create, read, update, rename, delete;
- verifies directory create/delete;
- verifies a Japanese filename using `lang=shift-jis`;
- unmounts the drive and stops the temporary server.

Expected result:

```text
PASS: Windows Client for NFS mount
PASS: Windows Client for NFS create/read/update/rename/delete
PASS: Windows Client for NFS directory create/delete
PASS: Windows Client for NFS Japanese filename
WINDOWS NFS CLIENT TEST PASSED
```

Use another drive letter:

```powershell
.\scripts\test-windows-nfs-client.ps1 -DriveLetter Y
```

Keep the temporary work folder after the test:

```powershell
.\scripts\test-windows-nfs-client.ps1 -KeepWork
```

## Manual Mount

The equivalent mount command is:

```cmd
mount -o anon,nolock,rsize=8,wsize=8,lang=shift-jis \\127.0.0.1\export Z:
```

Unmount:

```cmd
umount Z:
```

`nolock` is required because TinyWinNFS does not implement NLM.

## Windows Anonymous UID/GID

Windows Client for NFS anonymous mounts commonly use `UID=-2` and `GID=-2`. If the server reports `uid=0`, `gid=0`, and `file.mode=0644`, Windows may allow file creation but deny later updates to the same file.

For Windows-client integration tests without registry changes, use:

```properties
uid=-2
gid=-2
file.mode=0666
directory.mode=0777
filename.charset=Shift_JIS
```

The sample profile is `conf\nfs-server-windows-client-test.properties`.

## Troubleshooting

If `mount.exe` is missing, enable Windows Client for NFS.

If `NfsClnt` is not running:

```powershell
Start-Service NfsClnt
```

If `Access denied` occurs after file creation, verify that the test server is returning writable attributes for `UID=-2` / `GID=-2`.

If the mount command succeeds but Japanese filenames are garbled, verify that both sides use the same encoding:

- Windows mount option: `lang=shift-jis`
- TinyWinNFS config: `filename.charset=Shift_JIS`
