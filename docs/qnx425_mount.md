# QNX 4.25 mount notes

## Server

Default server mount name is `/export`.
Additional server mount names can be added in the manager app Share tab.

Default QNX mount point shown by the manager app is `/mnt`.

The Windows folder exposed through `/export` is configured by the manager app shared folder setting.
After changing it, save the configuration and restart the Windows service.

Default UDP ports:

- portmap: `111`
- nfsd: `2049`
- mountd: `20048`

Windows Firewall must allow these UDP ports.

## QNX 4.25

Before mounting, confirm that the QNX NFS filesystem manager is running:

```sh
ps -ef
```

If `NFSfsys` is not listed, start it:

```sh
NFSfsys &
```

Example mount command:

```sh
mount_nfs windows-host:/export /mnt
```

For another configured export name, mount that name:

```sh
mount_nfs windows-host:/work /mnt/work
```

If the mount command reports `No such process` after the Windows server has accepted the MOUNT request, the likely QNX-side cause is that `NFSfsys` is not running.

The manager app can edit these values and copy the generated `mount_nfs` command.

If the client supports mount options, prefer NFSv2 and UDP.

## Current Scope

The current implementation supports NFSv2 read-write operations over UDP and multiple configured exports.

Implemented NFSv2 procedures:

- `NULL`
- `ROOT`
- `GETATTR`
- `LOOKUP`
- `READLINK`
- `READ`
- `WRITECACHE`
- `WRITE`
- `CREATE`
- `REMOVE`
- `RENAME`
- `LINK`
- `SYMLINK`
- `MKDIR`
- `RMDIR`
- `READDIR`
- `STATFS`

NFS lock manager support is intentionally out of the current scope.
Filename encoding defaults to UTF-8 and can be changed with `filename.charset` if a legacy client requires another Java Charset.

TCP transport and lock manager are planned follow-up work. NFSv3 is implemented for newer clients, while QNX 4.25 remains validated through the NFSv2 path.
