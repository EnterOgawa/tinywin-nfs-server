# WSL mount テスト

WSL mount テストは任意の結合テストです。QNX 4.25 テストの代替ではありませんが、NFSv2/NFSv3 と UDP/TCP を実際の NFS クライアントから利用できることを確認する用途には有効です。

VMware 上の QNX 検証環境を優先する場合、WSL2 は Hyper-V 系の仮想化機能を使うため、ホスト環境によっては VMware 側の性能や安定性に影響します。その場合は Windows Client for NFS の検証を優先し、WSL は任意回帰として扱います。

## 現在の環境

このワークスペースには現在 WSL distribution がインストールされていないため、このテストはここでは実行できません。

## 前提条件

- Linux distribution が入った WSL2 がインストールされていること。
- WSL 内に Linux NFS クライアントツールがインストールされていること。
- Linux kernel が確認したい NFS version に対応していること。
- Windows ファイアウォールが UDP/TCP `11111`、`12049`、`12048` を許可していること。
- 環境依存テストのため、実行はユーザーから明示指示がある場合に限定すること。

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

NFSv3/UDP:

```sh
sudo mount -t nfs \
  -o vers=3,proto=udp,mountproto=udp,port=12049,mountport=12048,nolock,soft,timeo=5,retrans=1 \
  "$WIN_HOST:/export" /mnt/qnx-nfs-test
```

NFSv3/TCP:

```sh
sudo mount -t nfs \
  -o vers=3,proto=tcp,mountproto=tcp,port=12049,mountport=12048,nolock,soft,timeo=5,retrans=1 \
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

## 任意回帰マトリクス

| NFS | transport | 目的 |
|---|---|---|
| NFSv2 | UDP | QNX 4.25 に近い古いクライアント経路の補助確認 |
| NFSv3 | UDP | Windows Client for NFS と異なる Linux 実装での v3/UDP 確認 |
| NFSv3 | TCP | 新しいクライアント向けの v3/TCP 確認 |

確認項目:

- mount が成功する。
- `ls` と `cat` で既存ファイルを読める。
- 新規ファイル作成、更新、rename、delete が成功する。
- ディレクトリ作成と削除が成功する。
- Windows 側 export フォルダから内容、件数、サイズを確認できる。

## 注意

`mount.nfs` が UDP `111` の rpcbind へ接続し続ける場合は、管理者権限と既定設定を使用するか、packet capture とクライアント挙動をより直接制御できる Linux VM を使用します。

WSL が `/etc/resolv.conf` の Windows ホストアドレスに到達できない場合は、WSL が mirrored networking を使用しているかを確認し、WSL から見える Windows ホスト IP を使用します。

WSL 側で NFSv2 が無効な kernel の場合は、NFSv2/UDP の代替として QNX 4.25 実機確認を優先します。
