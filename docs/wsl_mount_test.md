# WSL mount test

WSL mount test is an optional integration test. It does not replace the QNX 4.25 test, but it is useful for checking that NFSv2/MOUNT v1 packets are usable by a real NFS client.

## Current Environment

This workspace currently has no WSL distribution installed, so this test cannot be executed here.

## Prerequisites

- WSL2 with a Linux distribution installed.
- Linux NFS client tools installed in WSL.
- Linux kernel NFSv2 client support.
- Windows Firewall allows UDP `11111`, `12049`, and `12048`.

Ubuntu example:

```sh
sudo apt update
sudo apt install -y nfs-common rpcbind
cat /proc/filesystems | grep nfs
```

## Start Server on Windows

Use the high-port WSL test configuration:

```powershell
.\scripts\start-wsl-test-server.ps1
```

This starts:

- portmap: UDP `11111`
- nfsd: UDP `12049`
- mountd: UDP `12048`

## Mount from WSL

In WSL, get the Windows host address:

```sh
WIN_HOST=$(awk '/nameserver/ {print $2; exit}' /etc/resolv.conf)
echo "$WIN_HOST"
```

Create a mount point:

```sh
sudo mkdir -p /mnt/qnx-nfs-test
```

Try an explicit NFSv2/UDP mount without lock manager:

```sh
sudo mount -t nfs \
  -o vers=2,proto=udp,mountproto=udp,port=12049,mountport=12048,nolock,soft,timeo=5,retrans=1 \
  "$WIN_HOST:/export" /mnt/qnx-nfs-test
```

Check read-write access:

```sh
ls -la /mnt/qnx-nfs-test
cat /mnt/qnx-nfs-test/README.txt
echo "wsl write test" | sudo tee /mnt/qnx-nfs-test/wsl-write-test.txt
cat /mnt/qnx-nfs-test/wsl-write-test.txt
```

Unmount:

```sh
sudo umount /mnt/qnx-nfs-test
```

## Notes

If `mount.nfs` still contacts rpcbind on UDP `111`, use administrator privileges and the default configuration instead, or use a Linux VM where packet capture and client behavior can be controlled more directly.

If WSL cannot reach the Windows host address from `/etc/resolv.conf`, check whether WSL is using mirrored networking and use the Windows host IP visible from WSL.
