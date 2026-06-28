# サポート範囲と既知制限

この文書は、TinyWinNFS Server v1.14.0 時点で 2.0.0 正式版に向けて固定するサポート範囲を整理します。

## 正式版で保証する範囲

| 区分 | 範囲 |
|---|---|
| NFS | NFSv2 / NFSv3 |
| MOUNT | MOUNT v1 / v2 / v3 |
| Portmap | Portmap v2 |
| 認証 | AUTH_NONE / AUTH_SYS |
| 通信方式 | UDP / TCP |
| export | 複数 export、読み書き、読み取り専用、許可クライアント制限 |
| 通常ファイル | lookup、read、write、create、remove、rename、setattr、getattr |
| ディレクトリ | mkdir、rmdir、readdir、readdirplus |
| NFSv3 情報応答 | access、fsstat、fsinfo、pathconf、commit |
| Windows サービス | WinSW による `TinyWinNfsServer` 常駐 |
| 管理ツール | SWT GUI、日本語/英語、共有設定、診断、ログ確認、mount支援 |
| 配布 | Java 21 runtime 同梱 Inno Setup インストーラー |

## 検証対象

| クライアント | 位置づけ | 主な確認 |
|---|---|---|
| QNX 4.25 | 重要な互換対象 | NFSv2/UDP、通常ファイル、ディレクトリ、大量コピー、削除 |
| Windows Client for NFS | Windows 標準クライアント | NFSv3/UDP、NFSv3/TCP、通常ファイル、ディレクトリ、日本語ファイル名 |
| Linux/WSL | 任意回帰 | NFSv2/UDP、NFSv3/UDP、NFSv3/TCP |

## 制限付き機能

| 機能 | 扱い |
|---|---|
| symlink | `READLINK` / `SYMLINK` は実装済み。ただし Windows の symlink 作成権限、Developer Mode、ファイルシステム機能に依存します。 |
| hard link | Windows が対象ファイルシステムで許可する場合に作成します。 |
| ファイル mode | NFS 属性として設定値と AUTH_SYS UID/GID を返します。Windows ACL の完全変換ではありません。 |
| UID/GID | `permission.identity=auto` ではクライアントの AUTH_SYS UID/GID を属性へ反映します。Windows の実ファイル所有者は変更しません。 |
| 大文字小文字 | Windows 側ファイルシステムの制約を受けます。大文字小文字のみ異なるパスは衝突する場合があります。 |
| 日本語ファイル名 | `filename.charset` とクライアント側 mount option の組み合わせに依存します。 |

## 対象外機能

| 機能 | 理由 |
|---|---|
| NLM / file locking | 現時点の主要対象では `nolock` 運用を前提にします。 |
| NFSv4 | NFSv2/NFSv3 とはプロトコル設計が異なるため対象外です。 |
| Kerberos / RPCSEC_GSS | AUTH_SYS を対象にします。 |
| Windows ACL 完全変換 | NFS mode と Windows ACL は同等ではないため対象外です。 |
| 特殊デバイスノード | Windows 上の通常ファイル/ディレクトリ/symlink/hard link を対象にします。 |
| pNFS / delegation | NFSv4 系の機能のため対象外です。 |

## 互換性の判断

- QNX 4.25 は、NFSv2/UDP の実機確認を最優先します。
- Windows Client for NFS は、サーバーログで MOUNT v3 / NFSv3 RPC と UDP/TCP を確認します。
- Linux/WSL は任意回帰です。VMware 環境に影響がある場合は実施しません。
- 実機確認は環境依存のため、ユーザーから明示指示がある場合のみ Codex 側で実行します。
