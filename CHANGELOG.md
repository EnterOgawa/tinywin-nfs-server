# 変更履歴

## 1.12.0 - 2026-06-28

- ローカルRPCベンチマークを追加し、NFSv2手続きの作成、書込、LOOKUP、READDIR、rename、削除をMarkdown/CSVへ記録できるようにしました。
- 長時間負荷ループ用の `test-long-running.ps1` を追加しました。
- UDP RPC ワーカー数、UDP ワーカーキューサイズ、TCP クライアントタイムアウトを設定ファイルから調整できるようにしました。
- NFSv2 大量READDIRの回帰テストと、書込キャッシュflushの回帰テストを追加しました。
- README、性能確認ドキュメント、v1.12.0リリースチェックリストを更新しました。

## 1.11.0 - 2026-06-28

- 管理ツールに診断タブを追加し、サービス状態、ポート状態、設定診断、export診断を一覧確認できるようにしました。
- サーバーログビューに検索、種別フィルタ、自動更新を追加し、大きいログでは末尾を非同期に読むようにしました。
- サービス操作結果に終了コード、標準出力、標準エラー、失敗理由の目安を表示するようにしました。
- 設定のインポート、エクスポート、既定値への初期化を管理ツールから実行できるようにしました。
- マウント支援表示を QNX、Windows Client for NFS、Linux/WSL 向けに分け、NFSv2/UDP、NFSv3/UDP、NFSv3/TCP の表示を整理しました。
- v1.11.0リリースチェックリストと管理ツールドキュメントを更新しました。

## 1.10.0 - 2026-06-28

- exportフォルダの運用診断を追加し、存在、種別、読込可否、書込可否、ファイル数、ディレクトリ数、総サイズを確認できるようにしました。
- Windowsファイルシステムで問題になりやすい大文字小文字のみ異なる相対パス衝突を検出できるようにしました。
- 設定診断を追加し、ポート重複、管理者権限が必要なポート、転送サイズ、ファイル名文字コード、許可クライアント未設定を警告・情報として整理しました。
- 管理ツールの診断パッケージに、環境情報、Java/OS情報、設定診断、export診断、`diagnostics/report.txt` を追加しました。
- Windowsファイルシステム制約とQNX運用注意のドキュメント、v1.10.0リリースチェックリストを追加しました。

## 1.9.0 - 2026-06-28

- Windows Client for NFS 結合テストで、実行環境、protocol、mount対象、操作結果、失敗理由をMarkdownレポートとして保存するようにしました。
- 管理ツールのサービス タブに `診断出力` を追加し、設定、バックアップ、TinyWinNFSログ、WinSWログ、サービス状態をZIPへまとめられるようにしました。
- NFSv2/NFSv3/MOUNT の手続きカバレッジ表を追加し、実装済み、互換 no-op、制限付き、未対応を整理しました。
- NFSv3 `FSINFO` / `FSSTAT` / `PATHCONF` の応答を、設定値とWindowsの容量情報に基づく内容へ改善しました。
- `write.size`、`directory.preferred.size`、`max.file.size`、`time.delta.nanos`、`pathconf.link.max`、`pathconf.name.max` 設定を追加しました。
- `smoke-service.ps1 -VerifyLargeTreeIntegrity` を拡張し、複数階層、28ファイル、rename、削除、残骸確認を行うようにしました。
- v1.9.0向けのリリースチェックリスト、README、管理ツール、Windows Client for NFS、Windowsサービス文書を更新しました。

## 1.8.0 - 2026-06-28

- Windows Client for NFS 検証スクリプトの前提条件チェックを強化し、`NfsClnt` が利用不能な状態では復旧案を表示して中断するようにしました。
- Windows Client for NFS 検証で、protocol 設定変更を行わない `-SkipProtocolChange` を追加しました。
- 管理ツールのサービス表示に、データルート、設定ファイル、既定 export、ログフォルダ、ポート状態、Windows Client for NFS 状態、設定バックアップフォルダを表示するようにしました。
- 管理ツールから TinyWinNFS ログと WinSW ログフォルダを開けるようにしました。
- 設定保存前と旧設定移行時に、設定ファイルを `conf\backups` 配下へ最大 10 世代バックアップするようにしました。
- `smoke-service.ps1 -VerifyLargeTreeIntegrity` を追加し、ディレクトリ階層と複数サイズのファイル整合性を任意で確認できるようにしました。
- QNX 4.25 実機回帰確認、Windows Client for NFS 注意点、リリース前のユーザー動作確認ゲートをドキュメント化しました。

## 1.7.1 - 2026-06-28

- インストール後の実行時設定、既定 export、サーバーログの配置先を `ProgramData\EnterOgawa\TinyWinNFS Server` に移動しました。
- 旧 `Program Files` 配下の設定を、新しい `ProgramData` のデータルートへ一度だけ移行する処理を追加しました。
- 設定ファイルが `conf` 配下にある場合、相対 `export.path` をデータルート基準で解決するようにしました。
- SWT 管理ツールのサービス診断表示で、アプリケーションルート、データルート、設定ファイル、サービス実行ファイル、ログファイルを個別に表示するようにしました。
- アプリ本体と可変データを分離する配置に合わせて、インストーラー、WinSW サービス設定、サービススクリプト、スモークテスト、ドキュメントを更新しました。
- Windows Client for NFS 検証スクリプトで、アンマウント後にテストドライブが解放されるまで待機するようにしました。
- README を表形式中心に整理し、設定、インストール後の配置、サービス、マウント、検証手順を参照しやすくしました。

## 1.7.0 - 2026-06-27

- NFSv2/NFSv3 `READLINK` を強化し、壊れた symlink や読み取れない symlink でもハンドラ例外ではなく安定した NFS ステータスを返すようにしました。
- NFSv3 `READLINK` にも、他のハンドルベース操作と同じクライアント許可リスト確認を追加しました。
- Windows の symlink 作成権限不足や不正なリンク先に対する NFSv2/NFSv3 `SYMLINK` のエラーマッピングを改善しました。
- NFSv2/NFSv3 の symlink 作成、壊れた symlink の `READLINK`、通常ファイルの `READLINK`、NFSv3 `MKNOD` の `NOTSUPP` 応答を回帰テストに追加しました。
- 同梱 Java ランタイムに `runtime\conf\security\java.security` が含まれるようにインストーラー包装を修正し、`Failed to launch JVM` が発生しないようにしました。
- `Program Files` インストールで管理者起動が必要なことを明確に示せるように、インストール後の管理ツールショートカットと設定保存時の扱いを改善しました。
- インストール後の管理ツールショートカットを GUI 実行ファイルへ戻し、Windows の `RUNASADMIN` 互換設定でコマンドプロンプトを表示せずに管理者起動するようにしました。
- link 互換方針と Windows symlink の制限事項を文書化しました。

## 1.6.1 - 2026-06-27

- AUTH_SYS の UID/GID に基づいて属性応答を自動調整する `permission.identity=auto` を追加しました。
- クライアント IP 別プロファイルなしで、Windows Client for NFS の直接マウントから作成したファイルを同じ匿名クライアントで更新できるように修正しました。
- NFS 属性応答時に export 全体の link 数を走査しないようにし、QNX 4.25 の大量コピー性能を改善しました。
- 大量コピー/削除時の負荷を下げるため、書込ファイルキャッシュ、非同期書込既定値、UDP 要求ワーカー分散、低頻度の運用ログ出力を追加しました。
- QNX のディレクトリ形式 `.nfsX*` silly rename クリーンアップ互換性を改善しました。
- AUTH_SYS 属性 identity、QNX WRITE 互換、再帰削除クリーンアップ、Windows Client for NFS の UDP/TCP 確認を回帰テストに追加しました。

## 1.6.0 - 2026-06-27

- ONC RPC over TCP の record marking に対応しました。
- 設定済みサービスポートを使う Portmap、MOUNT、NFS の TCP listener を追加しました。
- NFS v2/v3 と MOUNT v1-v3 の Portmap TCP mapping を追加しました。
- UDP と同じ実装で NFSv2/NFSv3 ファイル操作を TCP 経由でも確認しました。
- Windows Client for NFS の検証を UDP/TCP の両方へ拡張しました。
- TCP 対応に合わせて、ファイアウォール、サービス、インストーラー、ドキュメントを更新しました。

## 1.5.0 - 2026-06-27

- NFSv3 の weak cache consistency データを強化し、変更系応答で変更前に取得した属性を返すようにしました。
- NFSv3 `COMMIT` のファイル同期処理と操作ログを追加しました。
- export パスが存在しない、ディレクトリでない、読み取れない、書き込めない場合のサーバー側検証を強化しました。
- SWT 管理ツールで、検証済み一時ファイルへ設定を書き出してから既存設定を置き換えるようにしました。
- 存在しない export パスと NFSv3 WCC の変更前サイズを回帰テストに追加しました。
- サービス安定性テストスクリプトと v1.5.0 リリースチェックリストを追加しました。
- 運用安定化に合わせて README とインストーラー更新手順を更新しました。

## 1.4.0 - 2026-06-27

- 既存の NFSv2 実装に加えて、UDP 上の NFSv3 に対応しました。
- opaque file handle と AUTH_NONE/AUTH_SYS flavor を返す MOUNT v3 応答を追加しました。
- UDP 上の NFSv3 と MOUNT v3 の portmap 登録を追加しました。
- NFSv3 の read-write、メタデータ、ディレクトリ、ファイルシステム情報、commit 手続きを追加しました。
- Windows Client for NFS 検証で MOUNT v3 と NFSv3 RPC の到達を確認するようにしました。

## 1.3.0 - 2026-06-27

- MOUNT/NFS 要求に対する export ごとの IPv4 クライアント許可リストを追加しました。
- 不正な export 名、読み取れないフォルダ、書込可能 export に対する書込不可フォルダ指定の検証を強化しました。
- Save + Restart の結果、管理者権限確認、サービス実行ファイルパス、設定ファイルパスを明示するように管理ツールのサービス表示を改善しました。
- クライアントアドレス、XID、program、version、procedure、status、可能な場合は path を含む要求診断を改善しました。
- debug logging が有効でない限り、成功した NFS READ の要求レベルログを抑制するようにしました。

## 1.2.0 - 2026-06-27

- Windows 標準 NFS クライアントから TinyWinNFS をマウントする Windows Client for NFS 結合テストを追加しました。
- 匿名の書込可能 mount に使う Windows クライアント向けテスト設定プロファイルを追加しました。
- Windows Client for NFS の検証手順とリリースチェックリスト手順を文書化しました。
- パッケージ化した管理ツールイメージに Windows Client for NFS スモークテストスクリプトを含めました。

## 1.1.0 - 2026-06-27

- NFSv2 `WRITECACHE` を互換用 no-op として追加しました。
- Windows の read-only 属性へ対応する `SETATTR` mode 処理を改善しました。
- NFSv2 のファイル属性と `STATFS` 値の扱いを改善しました。
- ページ分割されたディレクトリ読取で `READDIR` cookie を安定化しました。
- Windows の `LINK` と `SYMLINK` の検証およびエラーマッピングを強化しました。
- ファイル名文字コードを設定可能にしました。

## 1.0.1 - 2026-06-27

- プロジェクトソースへ Apache License 2.0 を追加しました。
- パッケージ同梱コンポーネント向けの第三者ライセンス通知を追加しました。
- アプリイメージとインストーラーにライセンスファイルを含めました。

## 1.0.0 - 2026-06-27

- Windows ユーザー空間 NFSv2 サーバーを UDP 上に追加しました。
- MOUNT v1/v2 と AUTH_SYS に対応しました。
- QNX 4.25 互換向けの read-write ファイル操作を追加しました。
- 複数 export 設定を追加しました。
- 英語/日本語 UI に対応した SWT 管理ツールを追加しました。
- WinSW による Windows サービス連携を追加しました。
- Inno Setup インストーラー包装を追加しました。
