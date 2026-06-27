# WSL mount テスト

WSL mount テストは任意の結合テストです。QNX 4.25 テストの代替ではありませんが、NFSv2/MOUNT v1 packet が実際の NFS クライアントから利用できることを確認する用途には有効です。

## 現在の環境

このワークスペースには現在 WSL distribution がインストールされていないため、このテストはここでは実行できません。

## 前提条件

- Linux distribution が入った WSL2 がインストールされていること。
- WSL 内に Linux NFS クライアントツールがインストールされていること。
- Linux kernel が NFSv2 クライアントに対応していること。
- Windows ファイアウォールが UDP/TCP `11111`、`12049`、`12048` を許可していること。

Ubuntu の例:

```sh
sudo apt update
sudo apt install -y nfs-common rpcbind
cat /proc/filesystems | grep nfs
```

## Windows 側サーバー起動

高番ポートを使う WSL テスト設定で起動します。

```powershell
.\scripts\start-wsl-test-server.ps1
```

起動内容:

- portmap: UDP/TCP `11111`
- nfsd: UDP/TCP `12049`
- mountd: UDP/TCP `12048`

## WSL からの mount

WSL 内で Windows ホストアドレスを取得します。

```sh
WIN_HOST=$(awk '/nameserver/ {print $2; exit}' /etc/resolv.conf)
echo "$WIN_HOST"
```

mount point を作成します。

```sh
sudo mkdir -p /mnt/qnx-nfs-test
```

lock manager を使わず、NFSv2/UDP を明示して mount します。

```sh
sudo mount -t nfs \
  -o vers=2,proto=udp,mountproto=udp,port=12049,mountport=12048,nolock,soft,timeo=5,retrans=1 \
  "$WIN_HOST:/export" /mnt/qnx-nfs-test
```

読み書きアクセスを確認します。

```sh
ls -la /mnt/qnx-nfs-test
cat /mnt/qnx-nfs-test/README.txt
echo "wsl write test" | sudo tee /mnt/qnx-nfs-test/wsl-write-test.txt
cat /mnt/qnx-nfs-test/wsl-write-test.txt
```

unmount:

```sh
sudo umount /mnt/qnx-nfs-test
```

## 注意

`mount.nfs` が UDP `111` の rpcbind へ接続し続ける場合は、管理者権限と既定設定を使用するか、packet capture とクライアント挙動をより直接制御できる Linux VM を使用します。

WSL が `/etc/resolv.conf` の Windows ホストアドレスに到達できない場合は、WSL が mirrored networking を使用しているかを確認し、WSL から見える Windows ホスト IP を使用します。
