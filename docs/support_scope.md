# サポート範囲と既知制限

この文書は、TinyWinNFS Server v2.1.0 時点のサポート範囲と既知制限を整理します。

## 対応環境

| 項目 | 対象・検証状況 |
|---|---|
| Windows | Windows Pro / Windows Server 系の x64 構成を対象にします。版ごとの完全な互換性は保証しません。 |
| 主な開発環境 | Windows 11 Pro 系の x64。新しい OS ビルドでの実クライアント確認は別途必要です。 |
| Windows 10 / Windows Server | 個別の OS 版・サービス・クライアント環境で確認して採用してください。全ての版で検証済みという意味ではありません。 |
| CPU | SWT と同梱 runtime は x64 用です。32bit は対象外、ARM64 の x64 エミュレーションは未検証です。 |
| サービスラッパー | 次の配布では OS 側の .NET Framework 4.8 以降を使用する WinSW NET461 版を採用します。 |
| ファイルシステム | NTFS を主対象にします。他のファイルシステムや Windows 上のネットワークドライブは同等の検証範囲に含めません。 |

対応表は実装対象の機能を示します。製品の保守は最新安定版を中心とするベストエフォートで、応答期限、SLA、全環境での動作保証を意味しません。
公開済み v2.1.0 の検証対象と、以降の依存関係更新後の検証結果は区別して扱います。

## 対応する機能

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
| ファイル mode | 既定値またはクライアントの `SETATTR mode` を NFS 属性として返します。Windows ACL の完全変換ではありません。 |
| UID/GID | 新規作成時または `SETATTR uid/gid` で得た UID/GID を NFS メタデータとして返します。保存値がない場合は `permission.identity` に従います。Windows の実ファイル所有者は変更しません。 |
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
