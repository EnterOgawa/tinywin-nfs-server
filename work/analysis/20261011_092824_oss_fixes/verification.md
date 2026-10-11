# OSS 公開整備の確認結果

## 実施内容

確認日: 2026-10-11

| 確認 | 結果 |
|---|---|
| Java 21.0.11+10 のコンパイル | 成功 |
| PowerShell 7 の Unit テスト | 19件成功。通信テストなし |
| Windows PowerShell 5.1 の Unit テスト | 19件成功。通信テストなし |
| 新規展開した対応ソースからの依存取得・Unit テスト | 19件成功 |
| PowerShell 5.1 / 7 の全スクリプト構文 | エラーなし |
| CI 定義の actionlint | 成功。GitHub 上の CI は未実行 |
| JSON / XML の読み取り | 成功 |
| README・新規案内文書のローカルリンク | 欠落なし |
| 書式見本の Javadoc | `-Xdoclint:all -Werror` が警告なしで成功 |
| Inno Setup の `[Code]` 構文 | 実コードを使った検証用設定の ISCC コンパイル成功。検証用 exe は未実行 |
| プロジェクト外の出力先 | 拒否を確認 |
| 意図的に不一致にした依存の SHA-256 | 拒否し、最終配置しないことを確認 |
| WinSW NET461 版の取得 | 指定 SHA-256 と一致。実行していない |
| 対応ソース ZIP | 生成成功。7つの上流ソースアーカイブの内容ハッシュが全て一致 |
| ソース ZIP の再ビルド入力 | 本製品のソース、取得スクリプト、ライセンス、コンポーネント一覧、見本を確認 |
| `src` 配下の NFS 本体 | 差分なし |
| `git diff --check` | 成功 |

PowerShell 5.1 の確認中に日本語の文字コードによる構文問題が見つかったため、該当する PowerShell ファイルを UTF-8 BOM 付きへ変更して再確認しました。
書式見本の Javadoc も厳密確認で HTML 構造とコンストラクタ警告を修正し、再確認しました。

## 配布物

既存の署名済みインストーラーは作り直していません。

- ファイル: `dist/installer/TinyWinNfsSetup.exe`
- SHA-256: `CC6831FEC2B7B678037F04C3EB5D5C747B109D8C63514C0475C5A1C550FFA0CF`
- Authenticode: `Valid`

検証用の対応ソース ZIP は `work/tmp/oss-verification/TinyWinNfsSources.zip` に生成しました。

- SHA-256: `FD36977D21A9006D3BFD39E9AB77200F2D84CD89D7C22637FDD5161B717F698D`
- ZIP 内の項目数: 382
- 固定した第三者ソースアーカイブ数: 7
- 一時ファイルのため永続保管・公開 asset への採用は前提としません。

## 未実施・公開前に必要な確認

- 新しい app image と正式インストーラー全体の再作成・起動・インストール。
- WinSW NET461 版と .NET Framework 4.8 以降でのサービス登録、起動、停止、再起動、更新。
- TCP 通信テスト、QNX、Windows Client for NFS、WSL の mount と実クライアント確認。
- GitHub 上の CI 実行。
- 元の業務サンプルの過去履歴・タグについての公開許諾確認。
- GitHub の非公開脆弱性報告、リポジトリ説明、topics の反映。

ソース変更は `work/proposals/20261011_092824_oss_readiness_fixes.md` に記録しました。
コミット、push、tag、release 操作、署名処理、サービス操作は行っていません。既存の作業フォルダ移行に関する変更と解析記録を維持しています。
