# QNX 4.25 mount diagnosis

## Context

QNX 4.25 client can ping the Windows host, but mounting `fsnfserver:/export` to `/mnt` fails with:

```text
fsnfserver:/export on /mnt: No such process
```

## Windows server observations

The Windows service was running and received RPC calls from the QNX client `192.168.1.231`.

Observed calls:

```text
RPC 192.168.1.231:761 server=nfs-portmap program=100000 version=2 procedure=3 accept=0
RPC 192.168.1.231:760 server=nfs-portmap program=100000 version=2 procedure=3 accept=0
RPC 192.168.1.231:762 server=nfs-mount-v1 program=100005 version=1 procedure=1 accept=0
```

This means portmap lookup and MOUNT v1 `MNT` for `/export` reached the server and were accepted.
The failure occurs before the client performs NFSv2 filesystem operations such as `GETATTR`.

## Server updates

- Added RPC diagnostic logging for accepted/rejected calls.
- Added MOUNT program version 2 compatibility while keeping MOUNT v1 behavior.
- Added portmap replies for MOUNT v2.
- Added a unit test for MOUNT v2 MNT.

## Verification

```text
scripts/compile.ps1
scripts/test.ps1
scripts/smoke-service.ps1
```

Result:

```text
TEST PASSED: 6 tests
SERVICE SMOKE TEST PASSED
```

The Windows service was restarted after the changes.

## Likely next QNX-side checks

The Windows side is not rejecting the mount request. The most likely remaining cause is that the QNX NFS filesystem manager is not running, or the generic `mount` command is not invoking the NFS mount helper correctly.

Recommended QNX checks:

```sh
sin | grep NFS
NFSfsys &
mount_nfs 192.168.1.30:/export /mnt
```

If name resolution is known to work, this should also be valid:

```sh
mount_nfs fsnfserver:/export /mnt
```

Also confirm that `/mnt` exists before mounting.
