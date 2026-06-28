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

## 実機回帰確認

v1.8.0 以降のリリース前には、QNX 4.25 実機または同等の検証環境で以下を確認します。

| 観点 | QNX 側操作 | Windows サーバー側確認 |
|---|---|---|
| mount | `mount_nfs windows-host:/export /mnt` | `nfs-server.log` に MOUNT/NFS 要求が出る |
| ファイル作成 | `/mnt` 配下へ小さいテキストを作成 | export フォルダに同じ内容で作成される |
| ファイル更新 | QNX 側で追記または上書き | Windows 側から内容を読める |
| rename | QNX 側でファイル名変更 | 変更前が消え、変更後が存在する |
| delete | QNX 側でファイル削除 | Windows 側から消えている |
| ディレクトリ | QNX 側で mkdir/rmdir | Windows 側でも作成/削除される |
| ディレクトリコピー | 複数ファイルを含むフォルダを `/mnt` へコピー | 件数とサイズが一致する |
| 大量削除 | コピーしたフォルダを QNX 側から削除 | `.nfs*` 形式の残骸が残らない |
| symlink | symlink を含むコピーを実施 | 作成可否が Windows symlink 権限どおりで、代替通常ファイルを作らない |

Windows 側では、可能な範囲で件数と合計サイズを確認します。

```powershell
Get-ChildItem C:\ProgramData\EnterOgawa\TinyWinNFS Server\export -Recurse -File |
    Measure-Object -Property Length -Sum
```

比較元のディレクトリを Windows 側にも用意できる場合は、チェックサムで確認します。

```powershell
Get-ChildItem .\source -Recurse -File | Sort-Object FullName | Get-FileHash -Algorithm SHA256
Get-ChildItem .\export -Recurse -File | Sort-Object FullName | Get-FileHash -Algorithm SHA256
```

QNX 側で操作が失敗した場合は、以下を記録します。

- QNX 側のコマンドとエラーメッセージ。
- `ps -ef` の `NFSfsys` 状態。
- Windows 側 `C:\ProgramData\EnterOgawa\TinyWinNFS Server\logs\nfs-server.log` の該当時刻。
- WinSW ログ `C:\Program Files\EnterOgawa\TinyWinNFS Server\service\winsw` の該当時刻。
- 操作対象の export 名、Windows フォルダ、QNX mount point。

issue コメントへ転記する場合は、以下の形式を使います。

```text
QNX 4.25 実機確認:
- mount:
- create/update/read:
- rename/delete:
- directory copy:
- large copy/delete:
- symlink:
- Windows側件数/サイズ:
- 残課題:
```

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
