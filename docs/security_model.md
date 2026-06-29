# セキュリティとアクセス制限

この文書は、TinyWinNFS Server v2.1.0 時点のセキュリティ境界、アクセス制限、運用上の注意を整理します。

## 基本方針

TinyWinNFS Server は、信頼できる LAN 内で Windows フォルダを NFSv2/NFSv3 として公開する用途を想定しています。
インターネットへ直接公開する用途は対象外です。

## セキュリティ境界

| 境界 | 実装/運用 |
|---|---|
| export root | NFS 操作は登録済み export の配下に限定します。 |
| 複数 export | 重複 export 名、重複パス、ネストした export パスは設定検証で拒否します。 |
| クライアント制限 | `exports.N.allowed.clients` で IPv4 アドレス単位に制限します。 |
| 書込可否 | `exports.N.writable=false` の export は変更操作を拒否します。 |
| Windows 制約 | Windows が拒否する予約名、予約文字、権限不足は NFS エラーとして返します。 |
| サービス権限 | サービス実行アカウントが Windows ファイルシステム上で許可された範囲だけ操作できます。 |

## allowed clients

`exports.N.allowed.clients` はカンマ区切りの IPv4 アドレスです。

```properties
exports.1.allowed.clients=192.168.1.30,127.0.0.1
```

空欄の場合は全クライアントを許可します。
正式運用では、QNX 実機、Windows Client for NFS 検証端末、管理端末など、必要なクライアントだけを登録することを推奨します。

制限に一致しないクライアントからの MOUNT / NFS 要求は拒否されます。

## AUTH_SYS と Windows 権限

TinyWinNFS Server は AUTH_SYS の UID/GID を NFS 属性応答に反映できます。

```properties
permission.identity=auto
```

`auto` では、保存済み UID/GID メタデータがない場合に、現在のクライアントが送信した AUTH_SYS UID/GID を属性へ反映します。
新規作成時や `SETATTR uid/gid` では、対象パスの NFS UID/GID をメタデータとして保持し、後続の属性応答へ反映します。
これにより、QNX、Windows Client for NFS、Linux などがそれぞれ期待する ID を扱いやすくします。

ただし、これは Windows の実ファイル所有者や ACL を変更するものではありません。
実際にファイルを作成、更新、削除できるかは、TinyWinNFS Server のサービス実行アカウントと Windows ファイルシステム権限に依存します。

## NFS mode の扱い

| 設定 | 意味 |
|---|---|
| `file.mode` | 通常ファイルの既定 NFS 属性 mode |
| `directory.mode` | ディレクトリの既定 NFS 属性 mode |
| `exports.N.writable` | NFS 変更操作を許可するか |

NFS mode はクライアント向け属性応答です。クライアントが `chmod` / `SETATTR mode` を実行した場合、TinyWinNFS Server は対象パスの NFS mode をメタデータとして保持し、後続の `GETATTR` やディレクトリ表示へ反映します。
NFS UID/GID も同様にメタデータとして保持しますが、Windows ACL や Windows ファイル所有者の完全な変換ではありません。

Windows Client for NFS の匿名アクセスでは、`uid=-2` / `gid=-2` と `file.mode=0666` / `directory.mode=0777` が必要になる場合があります。

## 公開前確認

| 確認 | 方法 |
|---|---|
| 設定検証 | `.\scripts\test.ps1` の `Config validation` |
| 許可クライアント | `.\scripts\test.ps1` の `Client access restrictions` |
| export 診断 | 管理ツールの診断タブ |
| 大文字小文字衝突 | 診断パッケージの `diagnostics/report.txt` |
| 低番ポート使用 | 管理者権限と Windows Firewall 設定 |

## 運用上の注意

- `allowed.clients` が空欄のままの場合、同一ネットワーク上の任意のクライアントから mount 可能です。
- NLM/file locking は未対応のため、同じファイルを複数クライアントで同時編集する運用は避けます。
- Windows の大文字小文字非区別、予約名、予約文字は NFS クライアント側から見ても制約になります。
- symlink 作成は Windows 側の権限に依存します。失敗時に通常ファイルへフォールバックしません。
- firewall では UDP/TCP `111`、`2049`、`20048` の公開範囲を必要なネットワークに限定することを推奨します。
