# NFS 手続きカバレッジ

この表は TinyWinNFS Server v1.12.0 開発版時点の実装状況です。

## 区分

| 区分 | 意味 |
|---|---|
| 実装済み | 通常利用を想定した応答を実装済み |
| 互換 no-op | クライアント互換性のため成功応答するが、永続状態は変更しない |
| 制限付き | Windows の権限、ファイルシステム機能、または製品方針により制限あり |
| 未対応 | 明示的に未実装、または `PROC_UNAVAIL` / `NOTSUPP` 相当 |

## MOUNT v1-v3

| 手続き | 状態 | 備考 |
|---|---|---|
| `NULL` | 実装済み | 疎通確認 |
| `MNT` | 実装済み | export 名、許可クライアントを確認 |
| `DUMP` | 互換 no-op | 空リストを返す |
| `UMNT` | 互換 no-op | サーバー側 mount 状態を保持しない |
| `UMNTALL` | 互換 no-op | サーバー側 mount 状態を保持しない |
| `EXPORT` | 実装済み | 登録 export 一覧を返す |

## NFSv2

| 手続き | 状態 | 備考 |
|---|---|---|
| `NULL` | 実装済み | 疎通確認 |
| `GETATTR` | 実装済み | AUTH_SYS UID/GID 反映に対応 |
| `SETATTR` | 実装済み | サイズ、mode、mtime を中心に対応 |
| `ROOT` | 互換 no-op | 旧仕様互換 |
| `LOOKUP` | 実装済み | export 外参照と Windows 禁止名を拒否 |
| `READLINK` | 制限付き | Windows symlink のリンク先を返す。通常ファイルは `INVAL` |
| `READ` | 実装済み | 通常ファイル読込 |
| `WRITECACHE` | 互換 no-op | NFSv2互換の成功応答 |
| `WRITE` | 実装済み | QNX 4.25 互換のパディング差異に対応 |
| `CREATE` | 実装済み | 通常ファイル作成 |
| `REMOVE` | 実装済み | QNX の `.nfsX*` ディレクトリ削除互換を含む |
| `RENAME` | 実装済み | 上書き rename に対応 |
| `LINK` | 制限付き | Windows の hard link 機能に依存 |
| `SYMLINK` | 制限付き | Windows の symlink 作成権限に依存 |
| `MKDIR` | 実装済み | ディレクトリ作成 |
| `RMDIR` | 実装済み | 空ディレクトリ削除 |
| `READDIR` | 実装済み | cookie 分割に対応 |
| `STATFS` | 実装済み | Windows の `FileStore` 情報を元に応答 |

## NFSv3

| 手続き | 状態 | 備考 |
|---|---|---|
| `NULL` | 実装済み | 疎通確認 |
| `GETATTR` | 実装済み | post-op属性形式で応答 |
| `SETATTR` | 実装済み | サイズ、mode、mtime を中心に対応 |
| `LOOKUP` | 実装済み | export 外参照と Windows 禁止名を拒否 |
| `ACCESS` | 実装済み | 読込、lookup、書込、削除可否を返す |
| `READLINK` | 制限付き | Windows symlink のリンク先を返す |
| `READ` | 実装済み | 通常ファイル読込 |
| `WRITE` | 実装済み | stable/unstable 応答と書込キャッシュに対応 |
| `CREATE` | 実装済み | unchecked、guarded、exclusive の基本挙動に対応 |
| `MKDIR` | 実装済み | ディレクトリ作成 |
| `SYMLINK` | 制限付き | Windows の symlink 作成権限に依存 |
| `MKNOD` | 未対応 | 特殊デバイスノードは `NOTSUPP` |
| `REMOVE` | 実装済み | 通常ファイル削除、QNX 互換削除を含む |
| `RMDIR` | 実装済み | 空ディレクトリ削除 |
| `RENAME` | 実装済み | 上書き rename に対応 |
| `LINK` | 制限付き | Windows の hard link 機能に依存 |
| `READDIR` | 実装済み | cookie verifier に対応 |
| `READDIRPLUS` | 実装済み | 属性とファイルハンドルを返す |
| `FSSTAT` | 実装済み | Windows の `FileStore` 容量情報を元に応答 |
| `FSINFO` | 実装済み | `read.size`、`write.size`、`directory.preferred.size` などを反映 |
| `PATHCONF` | 実装済み | `pathconf.link.max`、`pathconf.name.max` を反映 |
| `COMMIT` | 実装済み | 書込キャッシュの同期に対応 |

## 重要な制限事項

| 項目 | 状態 | 備考 |
|---|---|---|
| NLM / file locking | 未対応 | QNX 4.25 と Windows Client for NFS では `nolock` 前提 |
| NFSv4 | 未対応 | NFSv2/NFSv3 を対象 |
| ACL / Windows権限完全変換 | 未対応 | NFS mode は設定値と AUTH_SYS を元に応答 |
| 特殊ファイル | 未対応 | Windows 上の通常ファイル/ディレクトリ/symlink/hard link を対象 |
| symlink 作成 | 制限付き | 管理者権限、Developer Mode、ファイルシステム機能に依存 |
| 日本語ファイル名 | 制限付き | クライアントごとに `filename.charset` と mount option を合わせる |

## 検証対象

| クライアント | 主な確認 |
|---|---|
| QNX 4.25 | NFSv2/UDP、通常ファイル、ディレクトリ、大量コピー、削除、Shift_JIS系ファイル名 |
| Windows Client for NFS | NFSv3/UDP、NFSv3/TCP、通常ファイル、ディレクトリ、日本語ファイル名 |
| WSL/Linux | 任意の結合確認。高番ポートでの mount 確認 |
