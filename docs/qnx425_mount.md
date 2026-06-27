# QNX 4.25 mount メモ

## サーバー

既定のサーバー側 mount 名は `/export` です。
追加のサーバー側 mount 名は、管理ツールの共有タブから登録できます。

管理ツールに表示する QNX 側の既定 mount point は `/mnt` です。

`/export` として公開する Windows フォルダは、管理ツールの共有フォルダ設定で指定します。
変更後は設定を保存し、Windows サービスを再起動してください。

既定の UDP/TCP ポート:

- portmap: `111`
- nfsd: `2049`
- mountd: `20048`

Windows ファイアウォールで、これらの UDP/TCP ポートを許可する必要があります。

## QNX 4.25

mount 前に、QNX の NFS filesystem manager が起動していることを確認します。

```sh
ps -ef
```

`NFSfsys` が表示されない場合は起動します。

```sh
NFSfsys &
```

mount コマンド例:

```sh
mount_nfs windows-host:/export /mnt
```

別の export 名を設定している場合は、その名前を mount します。

```sh
mount_nfs windows-host:/work /mnt/work
```

Windows サーバーが MOUNT 要求を受け付けているにもかかわらず mount コマンドが `No such process` を返す場合、QNX 側で `NFSfsys` が起動していない可能性が高いです。

管理ツールでは、これらの値を編集し、生成された `mount_nfs` コマンドをコピーできます。

クライアント側で mount option を指定できる場合は、NFSv2 と UDP を優先します。

## 現在の範囲

QNX 4.25 検証では、UDP 上の NFSv2 読み書き操作と複数 export 設定を確認します。

実装済み NFSv2 procedure:

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

NFS lock manager は、意図的に現在の範囲外としています。
ファイル名エンコードの既定値は UTF-8 です。古いクライアントで別の Java Charset が必要な場合は、`filename.charset` で変更できます。

新しいクライアント向けに TCP 通信方式も実装済みです。NFS lock manager は引き続き範囲外であり、QNX 4.25 は NFSv2/UDP 経路で検証します。

## リンク互換性

TinyWinNFS は、実際の Windows ファイルシステム上のリンクを NFS 経由で公開します。

- `READLINK` は、壊れた symlink を含め、実際の symlink に保存されたリンク先を返します。
- `SYMLINK` は、サービスアカウントとファイルシステムが許可する場合のみ Windows symlink を作成します。
- Windows が symlink 作成を拒否した場合、TinyWinNFS は NFS 失敗ステータスを返し、代替の通常ファイルは作成しません。
- `LINK` は、Windows が対象に対して対応している場合に hard link を作成します。

そのため、symlink を含む QNX ディレクトリコピーでは、Windows export 上に実際の symlink が作成されるか、その項目だけが失敗します。サーバー側ファイルツリーを壊すような代替通常ファイルは作成しません。device node などの特殊ファイルも通常ファイルとして代替しません。
