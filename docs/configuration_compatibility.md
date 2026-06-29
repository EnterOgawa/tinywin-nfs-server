# 設定互換性とデータ配置

この文書は、TinyWinNFS Server v2.1.0 時点の設定ファイル、データ配置、アップグレード方針を整理します。

## 標準配置

| 種別 | パス | 更新時の扱い |
|---|---|---|
| アプリ本体 | `C:\Program Files\EnterOgawa\TinyWinNFS Server` | インストーラーで置換 |
| 設定ファイル | `C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf\nfs-server.properties` | 維持 |
| 設定バックアップ | `C:\ProgramData\EnterOgawa\TinyWinNFS Server\conf\backups` | 維持、最大10世代 |
| 既定 export | `C:\ProgramData\EnterOgawa\TinyWinNFS Server\export` | `onlyifdoesntexist` で初回のみ作成 |
| TinyWinNFS ログ | `C:\ProgramData\EnterOgawa\TinyWinNFS Server\logs\nfs-server.log` | 維持 |
| 診断パッケージ | `C:\ProgramData\EnterOgawa\TinyWinNFS Server\diagnostics` | 維持 |
| NFS 属性メタデータ | `C:\ProgramData\EnterOgawa\TinyWinNFS Server\data\modes.properties` | mode、UID/GID を維持 |
| WinSW ログ | `C:\Program Files\EnterOgawa\TinyWinNFS Server\service\winsw` | アプリ本体側 |
| 同梱デフォルト設定 | `C:\Program Files\EnterOgawa\TinyWinNFS Server\defaults\conf` | インストーラーで置換 |

## 設定キーの互換性

| 方針 | 内容 |
|---|---|
| 追加キー | 既存設定に存在しない場合はコード側の既定値で動作します。 |
| 廃止予定キー | `export.*` は先頭 export との互換用として維持します。新規設定は `exports.N.*` を優先します。 |
| 不明キー | 現在の実装では使用しません。既存設定に残っていても読み込みを妨げません。 |
| 相対パス | `conf\nfs-server.properties` から見たデータルート基準で解決します。 |
| 既定 export | `exports.count=1`、`exports.1.name=/export`、`exports.1.path=export` を基本形とします。 |

## 主要設定

| キー | 既定値 | 互換性方針 |
|---|---|---|
| `portmap.port` | `111` | 既定値を維持 |
| `nfs.port` | `2049` | 既定値を維持 |
| `mount.port` | `20048` | 既定値を維持 |
| `rpc.udp.workers` | `8` | 省略時は既定値 |
| `rpc.udp.queue.size` | `1024` | 省略時は既定値 |
| `rpc.tcp.timeout.millis` | `30000` | 省略時は既定値 |
| `exports.count` | `0` | `0` の場合は `export.*` 互換設定を読む |
| `exports.N.name` | 必須 | `/` で始まる export 名 |
| `exports.N.path` | 必須 | 既存ディレクトリが必要 |
| `exports.N.writable` | `true` | 省略時は書込可 |
| `exports.N.allowed.clients` | 空欄 | 空欄は全クライアント許可 |
| `uid` / `gid` | `0` | `permission.identity=fixed` 時の属性値 |
| `permission.identity` | `auto` | `auto` または `fixed` |
| `file.mode` | `0644` | 通常ファイルの既定NFS属性応答値 |
| `directory.mode` | `0755` | ディレクトリの既定NFS属性応答値 |
| `read.size` | `8192` | NFS READ 応答上限 |
| `write.size` | `read.size` | NFSv3 FSINFO 応答値 |
| `write.sync` | `false` | 性能優先の既定値を維持 |
| `write.cache.enabled` | `true` | 性能優先の既定値を維持 |
| `filename.charset` | `UTF-8` | QNX などでは必要に応じて変更 |

## アップグレード時に維持するもの

- `ProgramData` 側の `conf\nfs-server.properties`。
- `conf\backups` 配下のバックアップ。
- export フォルダ内の利用者データ。
- `logs` と `diagnostics` 配下の運用ファイル。
- NFSクライアントから変更した mode、UID/GID メタデータ。
- 管理ツールから設定した複数 export。
- 許可クライアント、UID/GID、ファイル名文字コードなどの運用設定。

## 移行ルール

- 旧 `Program Files` 配下の `conf\nfs-server.properties` は、`ProgramData` 側に設定がまだ存在しない場合のみ移行します。
- 設定保存時と旧設定移行時には、既存設定を `conf\backups` 配下へ保存します。
- インストーラーは既定 export フォルダを初回のみ作成し、既存 export データを上書きしません。
- 旧サービス ID `OgawaNfsServer` / `QnxNfsServer` は移行用として停止・削除対象にします。

## アップグレード確認項目

| 確認 | 期待結果 |
|---|---|
| 上書きインストール | `TinyWinNfsServer` が停止、置換、再登録される |
| 設定ファイル | `ProgramData` 側の既存設定が維持される |
| export データ | 既存ファイルが残る |
| バックアップ | 設定保存時に最大10世代で保持される |
| 旧サービス | `OgawaNfsServer` / `QnxNfsServer` が残らない |
| 管理ツール | 設定保存後、サービス再起動で新設定が反映される |
